package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;
import com.educational.platform.courses.teacher.Teacher;
import com.educational.platform.courses.teacher.TeacherRepository;
import com.educational.platform.courses.teacher.create.CreateTeacherCommand;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.reset;

/**
 * End-to-end test that the {@code @Retryable} course command handlers, after a <em>real</em>
 * optimistic-lock conflict, reload the latest <em>committed</em> state on the retrying attempt and
 * compose their mutation on top of it - all the way through a real {@link CourseRepository}.
 *
 * <p>This closes a seam left by the existing coverage:
 * <ul>
 *   <li>{@code IncreaseNumberOfStudentsCommandHandlerRetryTest} /
 *       {@code CourseRetryApplicationContextWiringTest} prove the "increment on top of the
 *       concurrently-changed value" behaviour, but with a mocked repository whose successful
 *       {@code save} never writes - so the reloaded value is a stub, not a committed row.</li>
 *   <li>{@code CourseOptimisticLockingTest#saveAndFlush_reloadAfterConcurrentUpdate_*} exercises the
 *       real JPA reload-after-concurrent-commit recovery, but by hand - it never drives the
 *       {@code @Retryable} handler.</li>
 *   <li>{@code CourseRetryRecoveryIntegrationTest} drives the real handler against a real repository,
 *       but the retry reloads the <em>same</em> untouched row (0 -&gt; 1); nothing changes the
 *       persisted state between the failed and the recovering attempt.</li>
 * </ul>
 *
 * <p>Here the first {@code save} inside {@code handle()} (1) commits a genuine, versioned change to
 * the row from an independent {@link EntityManager} transaction - simulating another node winning the
 * race - and then (2) throws {@link ObjectOptimisticLockingFailureException}. The retrying attempt
 * therefore opens a fresh transaction, reloads the <em>committed</em> state (not the value the failed
 * attempt saw), applies the command, and persists. We assert the committed row reflects the
 * command's mutation layered on the concurrent writer's value, with the optimistic-lock version
 * advanced by both writes (0 -&gt; 1 by the concurrent writer, 1 -&gt; 2 by the recovery).
 */
@SpringBootTest
class CourseRetryConcurrentReloadRecoveryIntegrationTest {

    @MockitoSpyBean
    private CourseRepository courseRepository;

    // the spy's default answer delegates to the genuine Spring Data repository it wraps; reusing it
    // lets the recovering attempt persist for real after the first save has been stubbed
    private Answer<?> delegateToRealRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @BeforeEach
    void resetSpy() {
        // clear any stub left by a previous test before seeding (seeding saves through the spy)
        reset(courseRepository);
        delegateToRealRepository = mockingDetails(courseRepository)
                .getMockCreationSettings()
                .getDefaultAnswer();
    }

    @Test
    void increaseNumberOfStudents_retryReloadsConcurrentlyCommittedState_incrementsOnTopOfItAndPersists() {
        // given - a real persisted course at version 0 with no students
        final UUID uuid = persistCourse();

        // and - the first save loses to a concurrent writer that has already committed an increment
        // (number_of_students 0 -> 1, version 0 -> 1); the retry's save is real
        final AtomicInteger handleSaveAttempts = onFirstSaveCommitConcurrentlyThenConflict(
                () -> mutateInOwnTransaction(uuid, Course::increaseNumberOfStudents));

        // when
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - the handler retried exactly once
        assertThat(handleSaveAttempts.get()).isEqualTo(2);

        // and - the recovery reloaded the concurrently-committed value (1) rather than the value the
        // failed attempt saw (0), so the increment lands on top of it (-> 2); the version reflects
        // both versioned UPDATEs (concurrent 0 -> 1, recovery 1 -> 2)
        final Course reloaded = courseRepository.findByUuid(uuid).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(2));
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
    }

    @Test
    void updateCourseRating_retryReloadsConcurrentlyCommittedState_appliesCommandRatingAndPersists() {
        // given - a real persisted course at version 0 with rating 0
        final UUID uuid = persistCourse();

        // and - the first save loses to a concurrent writer that has already committed a different
        // rating (2.0, version 0 -> 1); the retry's save is real
        final AtomicInteger handleSaveAttempts = onFirstSaveCommitConcurrentlyThenConflict(
                () -> mutateInOwnTransaction(uuid, course -> course.updateRating(2.0)));

        // when
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 4.5));

        // then - the handler retried exactly once
        assertThat(handleSaveAttempts.get()).isEqualTo(2);

        // and - the recovery reloaded the concurrently-committed version (1) yet applies the command's
        // rating (4.5), not the reloaded 2.0; the version reflects both versioned UPDATEs (-> 2)
        final Course reloaded = courseRepository.findByUuid(uuid).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(2);
    }

    private AtomicInteger onFirstSaveCommitConcurrentlyThenConflict(Runnable concurrentCommit) {
        final AtomicInteger saveAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (saveAttempts.getAndIncrement() == 0) {
                // a concurrent transaction commits a versioned change to the same row, then this
                // attempt's save loses the race and surfaces the optimistic-lock conflict
                concurrentCommit.run();
                throw new ObjectOptimisticLockingFailureException(Course.class, 1);
            }
            // delegate to the real repository so the recovering attempt actually persists
            return delegateToRealRepository.answer(invocation);
        }).when(courseRepository).save(any(Course.class));
        return saveAttempts;
    }

    /**
     * Applies {@code mutation} to the course in a fresh, independent {@link EntityManager}
     * transaction and commits it - a genuine concurrent writer whose versioned UPDATE the handler's
     * in-flight attempt then loses to. Runs on its own connection so it commits while the handler's
     * (read-only-so-far) transaction is still open.
     */
    private void mutateInOwnTransaction(UUID uuid, java.util.function.Consumer<Course> mutation) {
        final EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();
            final Course course = em.createQuery("select c from Course c where c.uuid = :uuid", Course.class)
                    .setParameter("uuid", uuid)
                    .getSingleResult();
            mutation.accept(course);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private UUID persistCourse() {
        final Teacher teacher = teacherRepository.save(new Teacher(new CreateTeacherCommand("teacher")));
        final CreateCourseCommand command = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        final Course course = new Course(command, teacher.getId());
        courseRepository.save(course);
        return course.toIdentity();
    }
}
