package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the core testing infrastructure classes provided by
 * spring-boot-starter-test are available on the test classpath.
 * StarterTestWebUtilitiesValidationTest covers web-specific utilities;
 * this test covers the foundational testing annotations and utilities
 * that are required regardless of web-layer testing. If the
 * spring-boot-starter-test dependency is accidentally removed or its
 * scope changed, these tests will fail immediately.
 */
class SpringBootStarterTestCoreInfrastructureTest {

    @Test
    void springBootTestAnnotation_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName("org.springframework.boot.test.context.SpringBootTest");
            assertThat(clazz.isAnnotation())
                    .as("@SpringBootTest must be a resolvable annotation")
                    .isTrue();
        })
                .as("@SpringBootTest annotation must be on test classpath — "
                        + "it is the primary integration test annotation provided by starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void applicationContextRunner_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.context.runner.ApplicationContextRunner"))
                .as("ApplicationContextRunner must be on test classpath for "
                        + "lightweight auto-configuration testing without full context boot")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTestContextBootstrapper_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.context.SpringBootTestContextBootstrapper"))
                .as("SpringBootTestContextBootstrapper must be on test classpath — "
                        + "it is required for @SpringBootTest context initialization")
                .doesNotThrowAnyException();
    }

    @Test
    void mockitoCore_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.mockito.Mockito"))
                .as("Mockito must be on test classpath via starter-test transitive — "
                        + "required for mocking in Spring Boot integration tests")
                .doesNotThrowAnyException();
    }

    @Test
    void assertjCore_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.assertj.core.api.Assertions"))
                .as("AssertJ must be on test classpath via starter-test transitive — "
                        + "used throughout the test suite for fluent assertions")
                .doesNotThrowAnyException();
    }

    @Test
    void jsonAssert_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.skyscreamer.jsonassert.JSONAssert"))
                .as("JSONassert must be on test classpath via starter-test transitive — "
                        + "required for JSON response content assertions in web tests")
                .doesNotThrowAnyException();
    }

    @Test
    void hamcrestCore_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.hamcrest.Matchers"))
                .as("Hamcrest must be on test classpath via starter-test transitive — "
                        + "used by MockMvc result matchers and other test infrastructure")
                .doesNotThrowAnyException();
    }

    @Test
    void springTestContextFramework_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.context.TestContextManager"))
                .as("Spring TestContext framework must be on test classpath — "
                        + "it manages test lifecycle and dependency injection in tests")
                .doesNotThrowAnyException();
    }

    @Test
    void outputCaptureExtension_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.system.OutputCaptureExtension"))
                .as("OutputCaptureExtension must be on test classpath — "
                        + "used to capture and assert stdout/stderr in integration tests")
                .doesNotThrowAnyException();
    }
}
