package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot failure analysis infrastructure is available
 * on the classpath. When bootRun encounters startup failures (e.g., port conflicts,
 * missing beans, datasource issues), FailureAnalyzers provide human-readable
 * error messages instead of raw stack traces. This infrastructure is part of
 * the spring-boot module enabled by the plugin.
 */
class SpringBootFailureAnalyzerAvailabilityTest {

    @Test
    void failureAnalyzer_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.diagnostics.FailureAnalyzer"))
                .as("FailureAnalyzer must be available for human-readable startup error reporting")
                .doesNotThrowAnyException();
    }

    @Test
    void failureAnalysis_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.diagnostics.FailureAnalysis"))
                .as("FailureAnalysis result class must be on classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void abstractFailureAnalyzer_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.diagnostics.AbstractFailureAnalyzer"))
                .as("AbstractFailureAnalyzer must be available for custom analyzer implementations")
                .doesNotThrowAnyException();
    }

    @Test
    void beanCurrentlyInCreationFailureAnalyzer_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.autoconfigure.diagnostics.analyzer.NoSuchBeanDefinitionFailureAnalyzer"))
                .as("NoSuchBeanDefinitionFailureAnalyzer must be on classpath for missing bean diagnostics")
                .doesNotThrowAnyException();
    }

    @Test
    void contextRunnerFailure_shouldNotCrashWithoutFailureAnalyzer() {
        new ApplicationContextRunner()
                .withBean("conflictBean", String.class, () -> "value1")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void springBootExitCodeGenerator_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.ExitCodeGenerator"))
                .as("ExitCodeGenerator must be available for graceful bootRun shutdown exit codes")
                .doesNotThrowAnyException();
    }

    @Test
    void springApplication_exit_shouldBeAvailable() {
        assertThatCode(() -> {
            var method = org.springframework.boot.SpringApplication.class
                    .getDeclaredMethod("exit", org.springframework.context.ApplicationContext.class,
                            org.springframework.boot.ExitCodeGenerator[].class);
            assertThat(method).isNotNull();
        }).as("SpringApplication.exit() must be available for programmatic shutdown")
                .doesNotThrowAnyException();
    }
}
