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
 * End-to-end recovery test for the {@code @Retryable} course command handlers against a <em>real</em>
 * {@link CourseRepository} backed by an in-memory database.
 *
 * <p>Every other retry test in this suite mocks {@code CourseRepository.save(...)} for both the
 * conflicting attempt <em>and</em> the recovering attempt, so the "successful" save never actually
 * writes anything: those tests pin the retry mechanics (attempt counts, which exceptions are
 * retried) but cannot show that the recovery attempt persists correct state through the real
 * persistence path. This test boots the full application context (so the production
 * {@link CourseRetryConfiguration} {@code @EnableRetry} advisor is the one in effect), seeds a real
 * course, and uses a spy that throws {@link ObjectOptimisticLockingFailureException} on the first
 * {@code save} only - delegating to the real repository afterwards. It then reloads the row and
 * asserts the mutation the handler applied is the one actually committed, closing the gap between the
 * mocked retry tests and the {@code @DataJpaTest} optimistic-locking tests.
 */
@SpringBootTest
class CourseRetryRecoveryIntegrationTest {

    @MockitoSpyBean
    private CourseRepository courseRepository;

    // the spy's default answer delegates to the genuine Spring Data repository it wraps; reusing it
    // lets the recovering attempt persist for real after the first save has been stubbed to fail
    private Answer<?> delegateToRealRepository;

    @Autowired
    private TeacherRepository teacherRepository;

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
    void increaseNumberOfStudents_recoversFromSingleOptimisticLockFailure_persistsIncrementedStateToDatabase() {
        // given - a real persisted course at version 0
        final UUID uuid = persistCourse();

        // and - the first save inside handle() raises a real optimistic-lock conflict, the retry's save is real
        final AtomicInteger handleSaveAttempts = throwOptimisticLockOnFirstSaveThenPersist();

        // when
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - the handler retried exactly once (first attempt clashed, second persisted)
        assertThat(handleSaveAttempts.get()).isEqualTo(2);

        // and - the committed row reflects the increment applied on the recovering attempt, and the
        // versioned UPDATE advanced the optimistic-lock version from 0 to 1
        final Course reloaded = courseRepository.findByUuid(uuid).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
    }

    @Test
    void updateCourseRating_recoversFromSingleOptimisticLockFailure_persistsRatingToDatabase() {
        // given - a real persisted course at version 0
        final UUID uuid = persistCourse();

        // and - the first save inside handle() raises a real optimistic-lock conflict, the retry's save is real
        final AtomicInteger handleSaveAttempts = throwOptimisticLockOnFirstSaveThenPersist();

        // when
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 4.5));

        // then - the handler retried exactly once (first attempt clashed, second persisted)
        assertThat(handleSaveAttempts.get()).isEqualTo(2);

        // and - the committed row carries the rating from the command and the version advanced to 1
        final Course reloaded = courseRepository.findByUuid(uuid).orElseThrow();
        assertThat(reloaded).hasFieldOrPropertyWithValue("rating", new CourseRating(4.5));
        assertThat(ReflectionTestUtils.getField(reloaded, "version")).isEqualTo(1);
    }

    private AtomicInteger throwOptimisticLockOnFirstSaveThenPersist() {
        final AtomicInteger saveAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (saveAttempts.getAndIncrement() == 0) {
                throw new ObjectOptimisticLockingFailureException(Course.class, 1);
            }
            // delegate to the real repository so the recovering attempt actually persists
            return delegateToRealRepository.answer(invocation);
        }).when(courseRepository).save(any(Course.class));
        return saveAttempts;
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
