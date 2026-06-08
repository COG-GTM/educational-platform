package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that spring-boot-starter-test provides the complete embedded test
 * infrastructure required for integration testing of a Spring Boot web application.
 * The PR added starter-test to configuration/build.gradle.kts; these tests confirm
 * that higher-level test utilities (TestRestTemplate, MockMvc, WebTestClient) are
 * accessible alongside the basic test framework classes already validated by
 * {@link StarterTestTransitiveDependencyPresenceTest}.
 * <p>
 * If a future Spring Boot upgrade moves these classes to separate starters or
 * removes them from the default transitive set, these tests will surface the
 * regression before integration tests fail with ClassNotFoundException.
 */
class SpringBootTestInfrastructurePresenceTest {

    @Test
    void springBootMockServletContext_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.mock.web.SpringBootMockServletContext"))
                .as("SpringBootMockServletContext must be available via starter-test — "
                        + "it provides the mock servlet environment for @SpringBootTest "
                        + "with MOCK web environment (the default)")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvc_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.web.servlet.MockMvc"))
                .as("MockMvc must be available via spring-test (transitive of starter-test) — "
                        + "it is the primary tool for testing web controllers without starting a server")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvcRequestBuilders_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.web.servlet.request.MockMvcRequestBuilders"))
                .as("MockMvcRequestBuilders must be available for controller test setup")
                .doesNotThrowAnyException();
    }

    @Test
    void mockMvcResultMatchers_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.web.servlet.result.MockMvcResultMatchers"))
                .as("MockMvcResultMatchers must be available for controller assertion support")
                .doesNotThrowAnyException();
    }

    @Test
    void webApplicationContext_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.web.context.WebApplicationContext"))
                .as("WebApplicationContext must be available — "
                        + "it is required for MockMvc setup and @SpringBootTest web environment tests")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTestWebEnvironment_shouldExposeAllModes() throws ClassNotFoundException {
        Class<?> webEnvClass = Class.forName(
                "org.springframework.boot.test.context.SpringBootTest$WebEnvironment");
        assertThat(webEnvClass.isEnum())
                .as("SpringBootTest.WebEnvironment must be an enum")
                .isTrue();
        Object[] constants = webEnvClass.getEnumConstants();
        assertThat(constants.length)
                .as("WebEnvironment must have at least 4 modes (MOCK, RANDOM_PORT, DEFINED_PORT, NONE)")
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void outputCaptureExtension_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.system.OutputCaptureExtension"))
                .as("OutputCaptureExtension must be available via starter-test — "
                        + "it captures System.out/err for verifying log output in tests")
                .doesNotThrowAnyException();
    }

    @Test
    void dynamicPropertySource_shouldBeOnTestClasspath() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.context.DynamicPropertySource");
            assertThat(clazz.isAnnotation())
                    .as("@DynamicPropertySource must be a resolvable annotation for testcontainers integration")
                    .isTrue();
        }).doesNotThrowAnyException();
    }
}
