package com.educational.platform.courses;

import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommand;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommand;
import com.educational.platform.courses.course.rating.update.UpdateCourseRatingCommandHandler;

import org.junit.jupiter.api.Test;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the <em>shape</em> of the {@code @Backoff} the retry feature declares on both course command
 * handlers: a flat {@code 100ms} wait with no exponential growth, no cap and no jitter.
 *
 * <p>The {@code CourseRetryBackoffTest} timing assertions only check a lower bound on the elapsed
 * wait, and the per-handler {@code handle_isAnnotatedWithExpectedRetryableContract} tests only pin
 * {@code delay}. Neither would notice {@code @Backoff(delay = 100)} silently becoming, say,
 * {@code @Backoff(delay = 100, multiplier = 2)}: a fixed 100ms and an exponential 100ms->200ms both
 * satisfy a {@code >= 150ms} lower bound and an unchanged {@code delay}. This test asserts the
 * remaining {@code @Backoff} attributes ({@code multiplier}, {@code maxDelay}, {@code random}) stay
 * at their defaults so a change to exponential, capped or randomised backoff is caught.
 */
class CourseRetryBackoffShapeTest {

    @Test
    void increaseNumberOfStudentsHandler_usesFlatNonExponentialBackoff() throws NoSuchMethodException {
        final Method handle = IncreaseNumberOfStudentsCommandHandler.class
                .getMethod("handle", IncreaseNumberOfStudentsCommand.class);

        assertFlatHundredMillisBackoff(handle);
    }

    @Test
    void updateCourseRatingHandler_usesFlatNonExponentialBackoff() throws NoSuchMethodException {
        final Method handle = UpdateCourseRatingCommandHandler.class
                .getMethod("handle", UpdateCourseRatingCommand.class);

        assertFlatHundredMillisBackoff(handle);
    }

    private static void assertFlatHundredMillisBackoff(Method handle) {
        final Backoff backoff = handle.getAnnotation(Retryable.class).backoff();

        assertThat(backoff.delay()).as("flat 100ms wait between attempts").isEqualTo(100L);
        assertThat(backoff.multiplier()).as("no exponential growth").isEqualTo(0.0d);
        assertThat(backoff.maxDelay()).as("no delay cap").isZero();
        assertThat(backoff.random()).as("no jitter").isFalse();
    }
}
