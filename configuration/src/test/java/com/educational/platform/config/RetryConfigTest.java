package com.educational.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

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
}
