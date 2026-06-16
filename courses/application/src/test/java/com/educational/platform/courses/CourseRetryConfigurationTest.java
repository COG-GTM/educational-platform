package com.educational.platform.courses;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the production {@link CourseRetryConfiguration} (which carries {@code @EnableRetry})
 * actually activates the retry advisor for the {@code @Retryable} course command handlers.
 * Unlike the per-handler retry tests, this test wires the real configuration class so the
 * {@code @EnableRetry} declaration itself is exercised rather than a test-local copy.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetryConfigurationTest.HandlersConfig.class})
class CourseRetryConfigurationTest {

    @Configuration
    static class HandlersConfig {

        @Bean
        CourseRepository courseRepository() {
            return mock(CourseRepository.class);
        }

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler(CourseRepository repository) {
            return new IncreaseNumberOfStudentsCommandHandler(repository);
        }

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler(CourseRepository repository) {
            return new UpdateCourseRatingCommandHandler(repository);
        }
    }

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private CourseRepository repository;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    @Autowired
    private UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;

    @BeforeEach
    void resetMock() {
        reset(repository);
    }

    @Test
    void enableRetry_wrapsCommandHandlersInRetryAwareProxies() {
        // the @EnableRetry advisor from CourseRetryConfiguration proxies the @Retryable handlers
        assertThat(AopUtils.isAopProxy(increaseNumberOfStudentsCommandHandler)).isTrue();
        assertThat(AopUtils.isAopProxy(updateCourseRatingCommandHandler)).isTrue();
    }

    @Test
    void enableRetry_increaseNumberOfStudentsHandler_retriesOnOptimisticLockFailure() {
        // given - the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid));

        // then - the production configuration enabled retry, so the handler is re-invoked once
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(any(Course.class));
    }

    @Test
    void enableRetry_updateCourseRatingHandler_retriesOnOptimisticLockFailure() {
        // given - the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2));

        // then - the production configuration enabled retry, so the handler is re-invoked once
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(any(Course.class));
    }

    @Test
    void enableRetry_persistentOptimisticLock_exhaustsConfiguredAttemptsThenRethrows() {
        // given - every save clashes on the version under the real production configuration
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - the production @EnableRetry honours maxAttempts=3, then surfaces the original failure
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
    }

    @Test
    void enableRetry_nonRetryableException_isNotRetriedUnderProductionConfig() {
        // given - save fails with an exception outside the configured retryFor
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenThrow(new IllegalStateException("boom"));

        // when
        final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

        // then - the production retry advisor only retries ObjectOptimisticLockingFailureException
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> updateCourseRatingCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    @Test
    void enableRetry_missingCourse_resourceNotFoundIsNotRetriedUnderProductionConfig() {
        // given - the course does not exist
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final IncreaseNumberOfStudentsCommand command = new IncreaseNumberOfStudentsCommand(uuid);

        // then - ResourceNotFoundException is outside retryFor, so the lookup runs once and no save happens
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> increaseNumberOfStudentsCommandHandler.handle(command));
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, never()).save(any(Course.class));
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
