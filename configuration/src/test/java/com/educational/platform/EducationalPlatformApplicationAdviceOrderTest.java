package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the advisor ordering contract on the application class: async advice
 * must be the outer proxy so retries run on the executor thread.
 */
public class EducationalPlatformApplicationAdviceOrderTest {

    @Test
    void applicationClass_asyncAdviceOrderedBeforeRetryAdvice() {
        final EnableAsync enableAsync = EducationalPlatformApplication.class.getAnnotation(EnableAsync.class);
        final EnableRetry enableRetry = EducationalPlatformApplication.class.getAnnotation(EnableRetry.class);

        assertThat(enableAsync).isNotNull();
        assertThat(enableRetry).isNotNull();
        assertThat(enableAsync.order()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 1);
        assertThat(enableRetry.order()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        assertThat(enableAsync.order())
                .as("async advice must have higher precedence (lower order) than retry advice")
                .isLessThan(enableRetry.order());
    }

}
