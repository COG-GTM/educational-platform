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

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
