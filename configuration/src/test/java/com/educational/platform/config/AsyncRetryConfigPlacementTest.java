package com.educational.platform.config;

import com.educational.platform.EducationalPlatformApplication;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code @EnableAsync} and {@code @EnableRetry} are placed on their
 * respective dedicated configuration classes (not on the main application class),
 * and that their ordering ensures correct AOP proxy stacking:
 * <ul>
 *   <li>{@code @EnableAsync(order = HIGHEST_PRECEDENCE)} → async advisor wraps outermost</li>
 *   <li>{@code @EnableRetry(order = LOWEST_PRECEDENCE)} → retry advisor wraps innermost</li>
 * </ul>
 * This ordering means: caller → async proxy → retry proxy → actual method.
 * The retry mechanism runs entirely within the async thread, so retries don't
 * re-dispatch to the thread pool.
 */
class AsyncRetryConfigPlacementTest {

    @Test
    void enableAsync_isOnAsyncConfig_notOnApplicationClass() {
        assertThat(AsyncConfig.class.getAnnotation(EnableAsync.class))
                .as("@EnableAsync must be on AsyncConfig for centralized async configuration")
                .isNotNull();
        assertThat(EducationalPlatformApplication.class.getAnnotation(EnableAsync.class))
                .as("@EnableAsync must NOT be on the main application class (moved to AsyncConfig)")
                .isNull();
    }

    @Test
    void enableRetry_isOnRetryConfig_notOnApplicationClass() {
        assertThat(RetryConfig.class.getAnnotation(EnableRetry.class))
                .as("@EnableRetry must be on RetryConfig for centralized retry configuration")
                .isNotNull();
        assertThat(EducationalPlatformApplication.class.getAnnotation(EnableRetry.class))
                .as("@EnableRetry must NOT be on the main application class")
                .isNull();
    }

    @Test
    void asyncOrder_isHighestPrecedence_retryOrder_isLowestPrecedence() {
        int asyncOrder = AsyncConfig.class.getAnnotation(EnableAsync.class).order();
        int retryOrder = RetryConfig.class.getAnnotation(EnableRetry.class).order();

        assertThat(asyncOrder)
                .as("@EnableAsync order must be HIGHEST_PRECEDENCE (outermost advisor)")
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(retryOrder)
                .as("@EnableRetry order must be LOWEST_PRECEDENCE (innermost advisor)")
                .isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void asyncOrder_isStrictlyLessThanRetryOrder() {
        int asyncOrder = AsyncConfig.class.getAnnotation(EnableAsync.class).order();
        int retryOrder = RetryConfig.class.getAnnotation(EnableRetry.class).order();

        assertThat(asyncOrder)
                .as("Async advisor (order=%d) must wrap outside retry advisor (order=%d) "
                        + "so retries execute within the async thread", asyncOrder, retryOrder)
                .isLessThan(retryOrder);
    }

    @Test
    void enableAsync_andEnableRetry_areOnSeparateClasses() {
        assertThat(AsyncConfig.class.getAnnotation(EnableRetry.class))
                .as("AsyncConfig should NOT have @EnableRetry — separation of concerns")
                .isNull();
        assertThat(RetryConfig.class.getAnnotation(EnableAsync.class))
                .as("RetryConfig should NOT have @EnableAsync — separation of concerns")
                .isNull();
    }

    @Test
    void bothConfigs_areConfigurationClasses() {
        assertThat(AsyncConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class))
                .isNotNull();
        assertThat(RetryConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class))
                .isNotNull();
    }
}
