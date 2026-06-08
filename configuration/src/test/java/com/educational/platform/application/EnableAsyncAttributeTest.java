package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the @EnableAsync annotation attributes on the application entry point.
 * The Spring Boot plugin enables the full context to load, making async event
 * publishing functional. These tests guard against accidental attribute changes
 * that could alter proxying behavior for inter-module integration events.
 */
class EnableAsyncAttributeTest {

    @Test
    void enableAsync_shouldUseProxyMode() {
        EnableAsync annotation = EducationalPlatformApplication.class.getAnnotation(EnableAsync.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.mode().name())
                .as("@EnableAsync must use PROXY mode (default) for inter-module async event publishing; "
                        + "ASPECTJ mode requires additional weaving configuration")
                .isEqualTo("PROXY");
    }

    @Test
    void enableAsync_shouldNotForceProxyTargetClass() {
        EnableAsync annotation = EducationalPlatformApplication.class.getAnnotation(EnableAsync.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.proxyTargetClass())
                .as("proxyTargetClass should be false (default) to allow JDK interface-based proxies")
                .isFalse();
    }

    @Test
    void enableAsync_shouldUseDefaultOrder() {
        EnableAsync annotation = EducationalPlatformApplication.class.getAnnotation(EnableAsync.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.order())
                .as("@EnableAsync order must be LOWEST_PRECEDENCE (default) "
                        + "to not interfere with other AOP advisors")
                .isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }
}
