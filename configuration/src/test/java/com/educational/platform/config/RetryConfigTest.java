package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigTest {

    @Test
    void retryConfig_hasEnableRetryAnnotation() {
        // then
        assertThat(RetryConfig.class.getAnnotation(EnableRetry.class)).isNotNull();
    }

    @Test
    void retryConfig_hasConfigurationAnnotation() {
        // then
        assertThat(RetryConfig.class.getAnnotation(Configuration.class)).isNotNull();
    }

    @Test
    void retryConfig_canBeInstantiated() {
        // when
        RetryConfig config = new RetryConfig();

        // then
        assertThat(config).isNotNull();
    }

    @Test
    void enableRetry_hasLowestPrecedenceOrder() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.order()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void enableRetry_proxyTargetClassDefaultIsFalse() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        assertThat(enableRetry).isNotNull();
        assertThat(enableRetry.proxyTargetClass()).isFalse();
    }

    @Test
    void retryConfig_orderIsLowerThanAsyncOrder() {
        EnableRetry enableRetry = RetryConfig.class.getAnnotation(EnableRetry.class);
        EnableAsync enableAsync = AsyncConfig.class.getAnnotation(EnableAsync.class);
        assertThat(enableRetry.order()).isGreaterThan(enableAsync.order());
    }

    @Test
    void retryConfig_doesNotHaveEnableAsync() {
        assertThat(RetryConfig.class.getAnnotation(EnableAsync.class)).isNull();
    }

    @Test
    void retryConfig_hasNoDeclaredMethods() {
        assertThat(RetryConfig.class.getDeclaredMethods()).as("Pure config class should have no methods").isEmpty();
    }
}
