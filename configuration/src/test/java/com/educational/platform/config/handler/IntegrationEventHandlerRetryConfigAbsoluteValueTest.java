package com.educational.platform.config.handler;

import com.educational.platform.administration.course.create.SendCourseToApproveIntegrationEventHandler;
import com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;
import com.educational.platform.courses.course.rating.update.CourseRatingRecalculatedIntegrationEventHandler;
import com.educational.platform.courses.teacher.create.UserCreatedIntegrationEventHandler;

import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the absolute retry configuration values across all handlers.
 * Unlike the consistency test (which only ensures all handlers match each other),
 * this test ensures the actual production values match the design specification
 * from ADR-0014.
 */
class IntegrationEventHandlerRetryConfigAbsoluteValueTest {

    private static final List<Class<?>> ALL_HANDLER_CLASSES = List.of(
            SendCourseToApproveIntegrationEventHandler.class,
            CourseApprovedByAdminIntegrationEventHandler.class,
            StudentEnrolledToCourseIntegrationEventHandler.class,
            CourseRatingRecalculatedIntegrationEventHandler.class,
            UserCreatedIntegrationEventHandler.class
    );

    @Test
    void allHandlers_maxAttempts_isExactlyThree() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            assertThat(retryable.maxAttempts())
                    .as("@Retryable.maxAttempts in %s must be exactly 3 per ADR-0014",
                            handlerClass.getSimpleName())
                    .isEqualTo(3);
        }
    }

    @Test
    void allHandlers_backoffDelay_isExactly500ms() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.delay())
                    .as("@Backoff.delay in %s must be exactly 500ms per ADR-0014",
                            handlerClass.getSimpleName())
                    .isEqualTo(500L);
        }
    }

    @Test
    void allHandlers_backoffMultiplier_isExactlyTwo() {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Backoff backoff = method.getAnnotation(Retryable.class).backoff();
            assertThat(backoff.multiplier())
                    .as("@Backoff.multiplier in %s must be exactly 2.0 per ADR-0014",
                            handlerClass.getSimpleName())
                    .isEqualTo(2.0);
        }
    }

    @Test
    void allHandlers_maxAttemptsConstant_matchesAnnotationValue() throws Exception {
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Field maxAttemptsField = handlerClass.getDeclaredField("MAX_ATTEMPTS");
            maxAttemptsField.setAccessible(true);
            int constantValue = (int) maxAttemptsField.get(null);

            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);

            assertThat(constantValue)
                    .as("MAX_ATTEMPTS constant in %s must equal @Retryable.maxAttempts",
                            handlerClass.getSimpleName())
                    .isEqualTo(retryable.maxAttempts())
                    .isEqualTo(3);
        }
    }

    @Test
    void allHandlers_totalMaxBackoffTime_isUnderFiveSeconds() {
        // Backoff sequence: attempt1=0ms, retry1=500ms, retry2=1000ms → total max=1500ms
        for (Class<?> handlerClass : ALL_HANDLER_CLASSES) {
            Method method = findEventListenerMethod(handlerClass);
            Retryable retryable = method.getAnnotation(Retryable.class);
            Backoff backoff = retryable.backoff();

            long delay = backoff.delay();
            double multiplier = backoff.multiplier();
            int maxRetries = retryable.maxAttempts() - 1; // first attempt has no backoff
            long totalBackoff = 0;
            for (int i = 0; i < maxRetries; i++) {
                totalBackoff += (long) (delay * Math.pow(multiplier, i));
            }

            assertThat(totalBackoff)
                    .as("Total max backoff for %s should be under 5 seconds for fast failure recovery",
                            handlerClass.getSimpleName())
                    .isLessThan(5000L);
        }
    }

    private Method findEventListenerMethod(Class<?> handlerClass) {
        return Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getAnnotation(EventListener.class) != null)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No @EventListener method found in " + handlerClass.getSimpleName()));
    }
}
