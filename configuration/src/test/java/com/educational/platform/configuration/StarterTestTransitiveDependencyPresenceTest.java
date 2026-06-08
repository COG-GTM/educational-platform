package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that spring-boot-starter-test brings essential transitive
 * dependencies onto the test classpath. The PR added starter-test to the
 * configuration module; if a future Spring Boot upgrade drops a transitive
 * or changes its coordinates, these tests will surface the regression
 * before build failures occur in downstream test classes.
 * <p>
 * Complements {@link StarterTestTransitiveSafetyTest} (which guards against
 * unwanted transitives) and {@link SpringBootTestJUnitPlatformCompatibilityTest}
 * (which checks JUnit/Mockito extension compatibility).
 */
class StarterTestTransitiveDependencyPresenceTest {

    @Test
    void assertJ_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.assertj.core.api.Assertions"))
                .as("AssertJ Assertions must be on the test classpath — "
                        + "starter-test transitively includes assertj-core, "
                        + "which the entire test suite relies on for fluent assertions")
                .doesNotThrowAnyException();
    }

    @Test
    void mockitoCore_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.mockito.Mockito"))
                .as("Mockito must be on the test classpath — "
                        + "starter-test transitively includes mockito-core, "
                        + "used for mocking SpringApplication.run in main-method tests")
                .doesNotThrowAnyException();
    }

    @Test
    void hamcrest_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.hamcrest.Matchers"))
                .as("Hamcrest Matchers must be on the test classpath — "
                        + "starter-test transitively includes hamcrest for assertion support")
                .doesNotThrowAnyException();
    }

    @Test
    void jsonAssert_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName("org.skyscreamer.jsonassert.JSONAssert"))
                .as("JSONassert must be on the test classpath — "
                        + "starter-test transitively includes it for JSON comparison in API tests")
                .doesNotThrowAnyException();
    }

    @Test
    void springTestContext_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName(
                "org.springframework.test.context.TestContext"))
                .as("Spring TestContext framework must be on the test classpath — "
                        + "starter-test transitively includes spring-test")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTestAnnotation_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.boot.test.context.SpringBootTest");
            assertThat(clazz.isAnnotation())
                    .as("@SpringBootTest must be a resolvable annotation")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void mockBean_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(
                    "org.springframework.test.context.bean.override.mockito.MockitoBean");
            assertThat(clazz.isAnnotation())
                    .as("@MockitoBean must be a resolvable annotation for bean mocking in tests")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void applicationContextRunner_shouldBeAvailableViaStarterTest() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.context.runner.ApplicationContextRunner"))
                .as("ApplicationContextRunner must be available — "
                        + "it is used for auto-configuration testing without full context startup")
                .doesNotThrowAnyException();
    }
}
