package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that Spring Boot 4.x Ahead-of-Time (AOT) processing infrastructure
 * classes are available on the classpath. The Spring Boot plugin registers
 * {@code processAot} and {@code processTestAot} Gradle tasks that require
 * these classes to generate optimized bean definitions and configuration
 * metadata at build time. If these classes are missing — for example, due
 * to a dependency exclusion or incomplete BOM resolution — AOT processing
 * would fail at build time.
 * <p>
 * Spring Boot 4.x has made AOT a first-class feature; these tests ensure
 * that the boot plugin's AOT infrastructure is properly resolved via the
 * BOM and dependency management configuration.
 * <p>
 * Complements {@link BootPluginTaskRegistrationTest} (which validates
 * task availability) and {@link SpringBootAutoConfigurationTest}
 * (which validates auto-configuration). This test validates the AOT
 * compilation infrastructure that neither covers.
 */
class BootPluginAotInfrastructureAvailabilityTest {

    @Test
    void springAotProcessor_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.context.aot.ApplicationContextAotGenerator"))
                .as("ApplicationContextAotGenerator must be on the classpath — "
                        + "it is the core Spring Framework AOT processor used by the "
                        + "Spring Boot plugin's processAot task")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootAotRuntimeHints_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.aot.hint.RuntimeHints"))
                .as("RuntimeHints must be on the classpath — "
                        + "it is used to register reflection, resource, and proxy hints "
                        + "for GraalVM native-image compilation via the AOT pipeline")
                .doesNotThrowAnyException();
    }

    @Test
    void runtimeHintsRegistrar_shouldBeOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.aot.hint.RuntimeHintsRegistrar");
            assertThat(clazz.isInterface())
                    .as("RuntimeHintsRegistrar must be an interface that beans can implement "
                            + "to contribute custom AOT hints")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void beanFactoryInitializationAotProcessor_shouldBeOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor");
            assertThat(clazz.isInterface())
                    .as("BeanFactoryInitializationAotProcessor must be an interface — "
                            + "it is the SPI used by Spring Boot to generate AOT-optimized "
                            + "bean factory initialization code")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void beanRegistrationAotProcessor_shouldBeOnClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.beans.factory.aot.BeanRegistrationAotProcessor");
            assertThat(clazz.isInterface())
                    .as("BeanRegistrationAotProcessor must be an interface — "
                            + "it processes individual bean registrations during AOT compilation")
                    .isTrue();
        }).doesNotThrowAnyException();
    }
}
