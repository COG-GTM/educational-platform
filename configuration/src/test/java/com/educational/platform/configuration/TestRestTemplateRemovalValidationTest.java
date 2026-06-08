package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that Spring Boot 4.x test API removals are respected on the
 * test classpath. Spring Boot 4.0 removed {@code TestRestTemplate} in favor
 * of {@code RestClient} and {@code MockMvcTester}. If a dependency or
 * configuration accidentally pulls in a compatibility shim or an older
 * Spring Boot version, these removed classes would reappear, causing
 * confusion about which HTTP testing API to use.
 * <p>
 * Complements {@link TestRestClientAvailabilityTest} (validates RestClient
 * and MockMvcTester are available as replacements) and
 * {@link StarterTestTransitiveSafetyTest} (guards against unwanted transitives).
 */
class TestRestTemplateRemovalValidationTest {

    @Test
    void testRestTemplate_shouldNotBeOnTestClasspath() {
        assertThatThrownBy(() -> Class.forName(
                "org.springframework.boot.test.web.client.TestRestTemplate"))
                .as("TestRestTemplate was removed in Spring Boot 4.x — "
                        + "its presence would indicate a dependency pulling in "
                        + "an older Spring Boot version or a compatibility shim. "
                        + "Use RestClient or MockMvcTester instead.")
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void localServerPort_shouldBeAvailable() {
        try {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.web.server.LocalServerPort");
            assertThat(clazz.isAnnotation())
                    .as("@LocalServerPort must be available for embedded server port injection")
                    .isTrue();
        } catch (ClassNotFoundException e) {
            assertThat(false)
                    .as("@LocalServerPort must be on the test classpath via spring-boot-test")
                    .isTrue();
        }
    }

    @Test
    void restClient_shouldBeAvailableAsReplacement() {
        try {
            Class.forName("org.springframework.web.client.RestClient");
        } catch (ClassNotFoundException e) {
            assertThat(false)
                    .as("RestClient must be on the classpath as the Spring Boot 4.x "
                            + "replacement for TestRestTemplate")
                    .isTrue();
        }
    }

    @Test
    void mockMvcTester_shouldBeAvailableAsReplacement() {
        try {
            Class.forName("org.springframework.test.web.servlet.assertj.MockMvcTester");
        } catch (ClassNotFoundException e) {
            assertThat(false)
                    .as("MockMvcTester must be on the test classpath as the Spring Boot 4.x "
                            + "fluent assertion replacement for MockMvc result matching")
                    .isTrue();
        }
    }
}
