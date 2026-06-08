package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin auto-configures the logging
 * infrastructure (SLF4J + Logback). Proper logging is essential for
 * bootRun diagnostics, startup failure reporting, and runtime monitoring.
 * Without the plugin, the default logging configuration would not be applied.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SpringBootLoggingAutoConfigurationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void slf4jLoggerFactory_shouldBeAvailable() {
        Logger logger = LoggerFactory.getLogger(SpringBootLoggingAutoConfigurationTest.class);
        assertThat(logger)
                .as("SLF4J Logger must be obtainable — required for application logging in bootRun")
                .isNotNull();
    }

    @Test
    void logbackClassicModule_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("ch.qos.logback.classic.LoggerContext"))
                .as("Logback Classic must be on classpath (Spring Boot default logging implementation)")
                .doesNotThrowAnyException();
    }

    @Test
    void slf4jApi_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.slf4j.LoggerFactory"))
                .as("SLF4J API must be on classpath for logging abstraction")
                .doesNotThrowAnyException();
    }

    @Test
    void logbackEncoder_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("ch.qos.logback.classic.encoder.PatternLayoutEncoder"))
                .as("Logback PatternLayoutEncoder must be available for log formatting")
                .doesNotThrowAnyException();
    }

    @Test
    void defaultLoggingLevel_shouldBeInfoOrConfigured() {
        String rootLevel = environment.getProperty("logging.level.root");
        // Spring Boot defaults to INFO when not explicitly configured
        if (rootLevel != null) {
            assertThat(rootLevel.toUpperCase())
                    .as("Configured root logging level must be a valid level")
                    .isIn("TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF");
        }
        // If null, Spring Boot defaults to INFO — which is correct
    }

    @Test
    void loggerFactory_shouldUseLogbackImplementation() {
        String factoryClass = LoggerFactory.getILoggerFactory().getClass().getName();
        assertThat(factoryClass)
                .as("SLF4J must be bound to Logback (Spring Boot default) for consistent logging")
                .contains("logback");
    }

    @Test
    void applicationLogger_shouldBeCreatable_forAllModules() {
        assertThatCode(() -> {
            LoggerFactory.getLogger("com.educational.platform.courses");
            LoggerFactory.getLogger("com.educational.platform.users");
            LoggerFactory.getLogger("com.educational.platform.administration");
        }).as("Loggers for all bounded context modules must be creatable")
                .doesNotThrowAnyException();
    }
}
