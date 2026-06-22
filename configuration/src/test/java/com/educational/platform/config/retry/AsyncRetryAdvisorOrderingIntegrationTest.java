package com.educational.platform.config.retry;

import com.educational.platform.config.AsyncConfig;
import com.educational.platform.config.RetryConfig;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the advisor ordering configuration for @EnableAsync and @EnableRetry.
 * <p>
 * The intended contract is:
 * <ul>
 *   <li>{@code @EnableAsync(order = HIGHEST_PRECEDENCE)} — outermost advisor</li>
 *   <li>{@code @EnableRetry(order = LOWEST_PRECEDENCE)} — innermost advisor (closest to target)</li>
 * </ul>
 * This ensures that when both are active, the async proxy wraps the retry proxy
 * deterministically, so retries (including recovery) execute on the async thread.
 */
class AsyncRetryAdvisorOrderingIntegrationTest {

    @Test
    void asyncConfig_enableAsyncOrder_isHighestPrecedence() {
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableAsync).isNotNull();
        assertThat(enableAsync.order()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void retryConfig_enableRetryOrder_isLowestPrecedence() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.order()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void asyncConfigOrder_isLessThanRetryConfigOrder() {
        int asyncOrder = AsyncConfig.class.getAnnotation(EnableAsync.class).order();
        int retryOrder = RetryConfig.class.getAnnotation(EnableRetry.class).order();

        assertThat(asyncOrder)
                .as("Async order (outermost) should be less than Retry order (innermost)")
                .isLessThan(retryOrder);
    }

    @Test
    void asyncConfigOrder_isHighestPrecedenceExactValue() {
        int asyncOrder = AsyncConfig.class.getAnnotation(EnableAsync.class).order();
        assertThat(asyncOrder).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void retryConfigOrder_isLowestPrecedenceExactValue() {
        int retryOrder = RetryConfig.class.getAnnotation(EnableRetry.class).order();
        assertThat(retryOrder).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void asyncConfig_isConfigurationClass() {
        assertThat(AsyncConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class))
                .isNotNull();
    }

    @Test
    void retryConfig_isConfigurationClass() {
        assertThat(RetryConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class))
                .isNotNull();
    }

    @Test
    void asyncConfig_implementsAsyncConfigurer() {
        assertThat(org.springframework.scheduling.annotation.AsyncConfigurer.class)
                .isAssignableFrom(AsyncConfig.class);
    }
}
