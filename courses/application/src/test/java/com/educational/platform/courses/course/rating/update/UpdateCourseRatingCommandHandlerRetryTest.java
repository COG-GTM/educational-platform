package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
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
 * Verifies the {@code @Retryable} contract added to {@link UpdateCourseRatingCommandHandler}.
 * Uses a minimal Spring context with {@code @EnableRetry} so the retry proxy is actually applied.
 */
@SpringJUnitConfig
class UpdateCourseRatingCommandHandlerRetryTest {

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        CourseRepository courseRepository() {
            return mock(CourseRepository.class);
        }

        @Bean
        UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler(CourseRepository repository) {
            return new UpdateCourseRatingCommandHandler(repository);
        }
    }

    private final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private final UpdateCourseRatingCommand command = new UpdateCourseRatingCommand(uuid, 3.2);

    @Autowired
    private CourseRepository repository;

    @Autowired
    private UpdateCourseRatingCommandHandler sut;

    @BeforeEach
    void resetMock() {
        reset(repository);
    }

    @Test
    void handle_succeedsOnFirstAttempt_doesNotRetry() {
        // given - the save persists immediately, no version clash
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        sut.handle(command);

        // then - the happy path runs exactly once, the retry proxy does not re-invoke
        verify(repository, times(1)).findByUuid(uuid);
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(1)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(3.2));
    }

    @Test
    void handle_singleOptimisticLockThenSuccess_retriesOnce() {
        // given - only the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        sut.handle(command);

        // then - retry stops as soon as a save succeeds: exactly two attempts, fewer than maxAttempts
        verify(repository, times(2)).findByUuid(uuid);
        verify(repository, times(2)).save(any(Course.class));
    }

    @Test
    void handle_nonRetryableException_propagatesWithoutRetry() {
        // given - save fails with an exception outside retryFor
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class))).thenThrow(new IllegalStateException("boom"));

        // when
        final ThrowingCallable handle = () -> sut.handle(command);

        // then - only ObjectOptimisticLockingFailureException is retried, so this propagates on the first attempt
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(handle);
        verify(repository, times(1)).findByUuid(uuid);
        verify(repository, times(1)).save(any(Course.class));
    }

    @Test
    void handle_optimisticLockThenSuccess_retriesUntilSavePersists() {
        // given - the first two saves clash on the version, the third one succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        sut.handle(command);

        // then - each attempt reloads and re-applies the rating, total of three attempts
        verify(repository, times(3)).findByUuid(uuid);
        final ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(repository, times(3)).save(saved.capture());
        assertThat(saved.getValue()).hasFieldOrPropertyWithValue("rating", new CourseRating(3.2));
    }

    @Test
    void handle_persistentOptimisticLock_exhaustsThreeAttemptsThenThrows() {
        // given - every save clashes on the version
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1));

        // when
        final ThrowingCallable handle = () -> sut.handle(command);

        // then - the original exception is rethrown after maxAttempts is reached, no further attempts
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class).isThrownBy(handle);
        verify(repository, times(3)).findByUuid(uuid);
        verify(repository, times(3)).save(any(Course.class));
    }

    @Test
    void handle_missingCourse_resourceNotFoundExceptionIsNotRetried() {
        // given
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowingCallable handle = () -> sut.handle(command);

        // then - ResourceNotFoundException is not in retryFor, so it propagates on the first attempt
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
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
