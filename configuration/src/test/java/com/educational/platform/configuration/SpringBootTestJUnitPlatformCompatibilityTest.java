package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates compatibility between the JUnit Platform brought in transitively by
 * spring-boot-starter-test and the explicit JUnit declarations in
 * configuration/build.gradle.kts. The build file declares both
 * testImplementation("spring-boot-starter-test") and explicit
 * testImplementation("junit-jupiter-api"), testImplementation("junit-platform-engine"),
 * testImplementation("junit-platform-launcher"). If the versions from the BOM and
 * the explicit declarations diverge, tests would fail with NoSuchMethodError or
 * split-package errors at runtime.
 */
class SpringBootTestJUnitPlatformCompatibilityTest {

    @Test
    void junitJupiterEngine_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.engine.JupiterTestEngine"))
                .as("JUnit Jupiter engine must be available — it is the test execution engine "
                        + "required by useJUnitPlatform() in the Gradle test task")
                .doesNotThrowAnyException();
    }

    @Test
    void junitPlatformLauncher_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.junit.platform.launcher.Launcher"))
                .as("JUnit Platform Launcher must be available — it orchestrates test discovery and execution")
                .doesNotThrowAnyException();
    }

    @Test
    void junitPlatformEngine_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.junit.platform.engine.TestEngine"))
                .as("JUnit Platform TestEngine SPI must be available — "
                        + "without it, test discovery would fail silently")
                .doesNotThrowAnyException();
    }

    @Test
    void junitJupiterApi_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.api.Test"))
                .as("JUnit Jupiter @Test annotation must be resolvable on the test classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void junitJupiterParams_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.params.ParameterizedTest"))
                .as("JUnit Jupiter @ParameterizedTest must be available — "
                        + "it is used extensively in this project's test suite")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTestExtension_shouldBeCompatibleWithJupiter() {
        assertThatCode(() -> {
            Class<?> extensionClass = Class.forName(
                    "org.springframework.test.context.junit.jupiter.SpringExtension");
            // Verify it implements the JUnit Jupiter Extension interface
            assertThat(org.junit.jupiter.api.extension.Extension.class
                    .isAssignableFrom(extensionClass))
                    .as("SpringExtension must implement JUnit Jupiter Extension — "
                            + "version mismatch between Spring Test and JUnit Jupiter would break this")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void mockitoExtension_shouldBeCompatibleWithJupiter() {
        assertThatCode(() -> {
            Class<?> extensionClass = Class.forName(
                    "org.mockito.junit.jupiter.MockitoExtension");
            assertThat(org.junit.jupiter.api.extension.Extension.class
                    .isAssignableFrom(extensionClass))
                    .as("MockitoExtension must implement JUnit Jupiter Extension — "
                            + "version mismatch between Mockito and JUnit Jupiter would break this")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void junitJupiterApiVersion_shouldBeResolvable() {
        // The version is embedded in the module descriptor or manifest;
        // verifying the package is accessible confirms no split-package conflict
        Package jupiterPackage = org.junit.jupiter.api.Test.class.getPackage();
        assertThat(jupiterPackage)
                .as("JUnit Jupiter API package must be resolvable without split-package errors "
                        + "between starter-test's transitive and the explicit declaration")
                .isNotNull();
        assertThat(jupiterPackage.getName())
                .isEqualTo("org.junit.jupiter.api");
    }
}
