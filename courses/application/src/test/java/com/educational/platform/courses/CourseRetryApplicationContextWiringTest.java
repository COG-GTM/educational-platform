package com.educational.platform.courses;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the retry feature is wired correctly in the <em>real</em> Spring Boot application
 * context, exercising the production component-scan path end to end.
 *
 * <p>The sibling {@link CourseRetryConfigurationTest} proves the {@code @EnableRetry} +
 * {@code @Retryable} mechanics, but it hand-declares both the configuration and the handler beans
 * inside a sliced {@code @SpringJUnitConfig} context. That leaves one thing unproven: that the
 * production application actually <em>discovers</em> {@link CourseRetryConfiguration} via component
 * scanning and applies its retry advisor to the genuine {@code @Component} command handlers (which
 * are also {@code @Transactional}). This test boots the full context (so the real handler beans and
 * the real {@link CourseRetryConfiguration} are picked up by the {@code @SpringBootApplication}
 * scan) and asserts both the proxy wiring and the end-to-end retry behaviour through that wiring.
 *
 * <p>The {@link CourseRepository} is replaced with a Mockito bean so an optimistic-lock conflict can
 * be forced deterministically without a database, and the handler's own load -&gt; mutate -&gt; save
 * logic still runs against the real proxied bean.
 */
@SpringBootTest
class CourseRetryApplicationContextWiringTest {

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private CourseRepository repository;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @Test
    void productionContext_registersCourseRetryConfigurationBean() {
        // the @EnableRetry configuration must be reachable by the application's component scan,
        // otherwise the @Retryable advice would silently never be applied in production
        assertThat(applicationContext.getBeansOfType(CourseRetryConfiguration.class)).hasSize(1);
    }

    @Test
    void productionContext_commandHandlersAreRetryAwareProxies() {
        // the component-scanned handler beans must be AOP proxies carrying the retry (and transaction) advice
        assertThat(AopUtils.isAopProxy(increaseNumberOfStudentsCommandHandler)).isTrue();
        assertThat(AopUtils.isAopProxy(updateCourseRatingCommandHandler)).isTrue();
    }

    @Test
    void productionContext_increaseNumberOfStudentsHandler_conflictFreeSave_runsExactlyOnceAndIncrements() {
        // given - the save persists immediately through the production-scanned proxy, no version clash
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - invoking the real component-scanned bean
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - the production retry advisor must not re-invoke the happy path; the increment is persisted once
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void productionContext_updateCourseRatingHandler_conflictFreeSave_runsExactlyOnceAndUpdatesRating() {
        // given - the save persists immediately through the production-scanned proxy, no version clash
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - invoking the real component-scanned bean
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - the production retry advisor must not re-invoke the happy path; the new rating is persisted once
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(3.2));
    }

    @Test
    void productionContext_increaseNumberOfStudentsHandler_retriesOnOptimisticLockFailure() {
        // given - the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when - invoking the real component-scanned bean
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - the production wiring re-invokes the handler once and persists the increment
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(1));
    }

    @Test
    void productionContext_updateCourseRatingHandler_retriesOnOptimisticLockFailure() {
        // given - the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when - invoking the real component-scanned bean
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - the production wiring re-invokes the handler once and persists the command's rating
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(3.2));
    }

    @Test
    void productionContext_increaseNumberOfStudentsHandler_retryReloadsConcurrentState_incrementsOnTopOfReloadedValue() {
        // given - the first attempt loads a course with 5 students and clashes on save; before the
        // retry reloads, a concurrent writer commits a 6th student, so the second findByUuid returns
        // the updated state
        when(repository.findByUuid(uuid))
                .thenReturn(Optional.of(courseWithStudents(5)))
                .thenReturn(Optional.of(courseWithStudents(6)));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when - invoking the real component-scanned bean
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - through the production-scanned proxy each attempt re-derives the increment from its own
        // fresh reload, so the persisted value composes on top of the concurrent writer's commit (6 + 1)
        // rather than the stale snapshot (5 + 1) or accumulating across attempts. The existing
        // full-context retry tests reload an identical fresh course on every attempt, so they cannot
        // catch a wiring bug that reuses the stale first-attempt state instead of the second reload.
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("numberOfStudents", new NumberOfStudents(7));
    }

    @Test
    void productionContext_updateCourseRatingHandler_retryReloadsConcurrentState_appliesCommandRatingNotReloadedValue() {
        // given - the first attempt loads a course rated 1.0 and clashes on save; the retry reloads the
        // course as a concurrent writer left it (rated 4.5) and then succeeds
        when(repository.findByUuid(uuid))
                .thenReturn(Optional.of(courseWithRating(1.0)))
                .thenReturn(Optional.of(courseWithRating(4.5)));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when - invoking the real component-scanned bean
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - the rating is an idempotent overwrite taken from the command, so through the production
        // proxy the persisted value is the command's 3.2 on every attempt - never the stale snapshot (1.0)
        // nor the concurrent writer's value (4.5). The existing full-context tests reload an identical
        // rating-0 course on every attempt, so they cannot prove the per-attempt re-application.
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(3.2));
    }

    @Test
    void productionContext_updateCourseRatingHandler_persistentOptimisticLock_exhaustsConfiguredAttemptsThenRethrows() {
        // given - every save clashes on the version
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

        // then - the rating write path honours maxAttempts=3 through the real proxied bean too, then rethrows
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
    }

    @Test
    void productionContext_persistentOptimisticLock_exhaustsConfiguredAttemptsThenRethrows() {
        // given - every save clashes on the version
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - the production wiring honours maxAttempts=3 then surfaces the original failure
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
    }

    @Test
    void productionContext_increaseNumberOfStudentsHandler_missingCourse_resourceNotFoundIsNotRetried() {
        // given - the course is absent, so the handler throws before any save happens
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - ResourceNotFoundException sits outside retryFor, so the production proxy must surface it
        // on the first attempt rather than burning the configured retries on a non-transient failure
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, never()).save(any(Course.class));
    }

    @Test
    void productionContext_updateCourseRatingHandler_missingCourse_resourceNotFoundIsNotRetried() {
        // given - the course is absent, so the handler throws before any save happens
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

        // then - ResourceNotFoundException sits outside retryFor, so the production proxy must surface it
        // on the first attempt rather than burning the configured retries on a non-transient failure
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, never()).save(any(Course.class));
    }

    @Test
    void productionContext_increaseNumberOfStudentsHandler_nonRetryableSaveFailure_propagatesWithoutRetry() {
        // given - the save fails with an exception that is not the configured ObjectOptimisticLockingFailureException
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenThrow(new IllegalStateException("boom"));

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - the production retry advisor must only retry on the declared optimistic-lock failure, so an
        // unrelated exception propagates after a single attempt (guards against the wiring widening retryFor)
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    @Test
    void productionContext_updateCourseRatingHandler_nonRetryableSaveFailure_propagatesWithoutRetry() {
        // given - the save fails with an exception that is not the configured ObjectOptimisticLockingFailureException
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenThrow(new IllegalStateException("boom"));

        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

        // then - the production retry advisor must only retry on the declared optimistic-lock failure, so an
        // unrelated exception propagates after a single attempt (guards against the wiring widening retryFor)
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }

    private static Course courseWithStudents(int count) {
        final Course course = newCourse();
        for (int i = 0; i < count; i++) {
            course.increaseNumberOfStudents();
        }
        return course;
    }

    private static Course courseWithRating(double rating) {
        final Course course = newCourse();
        course.updateRating(rating);
        return course;
    }
}
