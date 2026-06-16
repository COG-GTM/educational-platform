package com.educational.platform.courses;

import com.educational.platform.courses.course.Course;
import com.educational.platform.courses.course.CourseRepository;
import com.educational.platform.courses.course.create.CreateCourseCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the {@code @Backoff(delay = 100)} declared on the course command handlers is
 * actually honoured at runtime under the production {@link CourseRetryConfiguration}
 * ({@code @EnableRetry}), rather than merely being present as an annotation value.
 *
 * <p>The sibling retry tests count attempts and assert the {@code backoff.delay()} attribute, but
 * none observe that consecutive attempts are <em>spaced apart</em>. Without a backoff policy a hot
 * optimistic-lock conflict would be retried in a tight loop, defeating the point of the delay
 * (giving the conflicting transaction time to commit). A single retry must therefore introduce one
 * ~100ms pause before the second attempt.
 *
 * <p>The assertion uses a generous lower bound rather than an exact duration so it stays
 * deterministic: a configured fixed-delay sleep never returns early, so the elapsed time
 * comfortably distinguishes "backoff applied (~100ms)" from "no backoff (~0ms)" without being
 * sensitive to scheduling jitter on a loaded CI machine.
 */
@SpringJUnitConfig(classes = {CourseRetryConfiguration.class, CourseRetryBackoffTest.HandlersConfig.class})
class CourseRetryBackoffTest {

    private static final long CONFIGURED_BACKOFF_MILLIS = 100L;
    // lower bound only: catches a missing/zero backoff (~0ms) while tolerating clock granularity
    private static final long MIN_OBSERVED_BACKOFF_MILLIS = 50L;

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
    void increaseNumberOfStudentsHandler_singleRetry_waitsConfiguredBackoffBetweenAttempts() {
        // given - the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        final long elapsed = timeHandle(() ->
                increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(uuid)));

        // then - exactly one retry occurred and the @Backoff delayed the second attempt
        verify(repository, times(2)).save(any(Course.class));
        assertThat(elapsed)
                .as("a single retry must wait the configured ~%dms backoff before re-attempting",
                        CONFIGURED_BACKOFF_MILLIS)
                .isGreaterThanOrEqualTo(MIN_OBSERVED_BACKOFF_MILLIS);
    }

    @Test
    void updateCourseRatingHandler_singleRetry_waitsConfiguredBackoffBetweenAttempts() {
        // given - the first save clashes on the version, the retry succeeds
        when(repository.findByUuid(uuid)).thenAnswer(invocation -> Optional.of(newCourse()));
        when(repository.save(any(Course.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Course.class, 1))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        final long elapsed = timeHandle(() ->
                updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(uuid, 3.2)));

        // then - exactly one retry occurred and the @Backoff delayed the second attempt
        verify(repository, times(2)).save(any(Course.class));
        assertThat(elapsed)
                .as("a single retry must wait the configured ~%dms backoff before re-attempting",
                        CONFIGURED_BACKOFF_MILLIS)
                .isGreaterThanOrEqualTo(MIN_OBSERVED_BACKOFF_MILLIS);
    }

    private static long timeHandle(Runnable handle) {
        final long start = System.nanoTime();
        handle.run();
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static Course newCourse() {
        final CreateCourseCommand createCourseCommand = CreateCourseCommand.builder()
                .name("name")
                .description("description")
                .build();
        return new Course(createCourseCommand, 15);
    }
}
