package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot environment customization SPIs are available
 * on the classpath. These interfaces are core extension points that allow
 * customizing the {@code Environment} before the application context is
 * refreshed. The Spring Boot plugin enables these mechanisms during bootRun;
 * the test infrastructure (via starter-test) uses them for test property
 * overrides and profile activation.
 * <p>
 * Complements {@link SpringBootEnvironmentConfigurationTest} (which tests
 * environment property resolution) and {@link SpringBootConfigurationPropertiesBindingTest}
 * (which tests property binding). This test validates the SPI contracts
 * themselves — specifically the Spring Boot 4.x environment customization
 * entry points.
 */
class SpringBootTestEnvironmentPostProcessorTest {

    @Test
    void applicationContextInitializer_shouldBeOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.context.ApplicationContextInitializer");
            assertThat(clazz.isInterface())
                    .as("ApplicationContextInitializer must be an interface — "
                            + "it is a core SPI for customizing the application context before refresh")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void springApplicationRunListener_shouldBeOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.SpringApplicationRunListener");
            assertThat(clazz.isInterface())
                    .as("SpringApplicationRunListener must be an interface — "
                            + "it is the SPI for listening to SpringApplication lifecycle events")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void configurableEnvironment_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.core.env.ConfigurableEnvironment"))
                .as("ConfigurableEnvironment must be on the classpath — "
                        + "it is the mutable environment type used for property customization")
                .doesNotThrowAnyException();
    }

    @Test
    void propertySourceLoader_shouldBeOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.env.PropertySourceLoader");
            assertThat(clazz.isInterface())
                    .as("PropertySourceLoader must be an interface — "
                            + "it defines how property files (.properties, .yml) are loaded")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void springApplicationBuilder_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.builder.SpringApplicationBuilder"))
                .as("SpringApplicationBuilder must be on the classpath — "
                        + "it provides the fluent API for configuring SpringApplication instances")
                .doesNotThrowAnyException();
    }
}
