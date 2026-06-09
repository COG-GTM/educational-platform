package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the annotation metadata of {@code @SpringBootTest} and related
 * test infrastructure annotations provided by spring-boot-starter-test.
 * These annotations must have {@code RUNTIME} retention for the JUnit
 * Platform to discover and process them during test execution. If a
 * future Spring Boot version accidentally changed retention to
 * {@code CLASS} or {@code SOURCE}, annotated test classes would compile
 * but silently be ignored by the test runner.
 * <p>
 * Complements {@link StarterTestTransitiveDependencyPresenceTest} (classpath
 * availability) and {@link SpringBootTestWebEnvironmentCompletenessTest}
 * (WebEnvironment enum completeness). This test validates annotation-level
 * metadata contracts that existing tests do not cover.
 */
class SpringBootTestAnnotationRetentionTest {

    @Test
    void springBootTest_shouldHaveRuntimeRetention() {
        Retention retention = SpringBootTest.class.getAnnotation(Retention.class);
        assertThat(retention)
                .as("@SpringBootTest must declare @Retention — "
                        + "without it, the annotation defaults to CLASS retention "
                        + "and would not be visible at runtime for test discovery")
                .isNotNull();
        assertThat(retention.value())
                .as("@SpringBootTest must have RUNTIME retention for JUnit Platform "
                        + "to discover annotated test classes via reflection")
                .isEqualTo(RetentionPolicy.RUNTIME);
    }

    @Test
    void mockitoBean_shouldHaveRuntimeRetention() {
        assertThatCode(() -> {
            Class<?> mockitoBeanClass = Class.forName(
                    "org.springframework.test.context.bean.override.mockito.MockitoBean");
            Retention retention = mockitoBeanClass.getAnnotation(Retention.class);
            assertThat(retention)
                    .as("@MockitoBean must declare @Retention")
                    .isNotNull();
            assertThat(retention.value())
                    .as("@MockitoBean must have RUNTIME retention for the Spring "
                            + "TestContext Framework to detect and process bean overrides")
                    .isEqualTo(RetentionPolicy.RUNTIME);
        }).doesNotThrowAnyException();
    }

    @Test
    void springBootTestWebEnvironment_shouldHaveAllFourConstants() {
        SpringBootTest.WebEnvironment[] values = SpringBootTest.WebEnvironment.values();
        assertThat(values)
                .as("WebEnvironment enum must have exactly four constants "
                        + "(MOCK, RANDOM_PORT, DEFINED_PORT, NONE)")
                .hasSize(4);
        assertThat(values)
                .extracting(Enum::name)
                .containsExactlyInAnyOrder("MOCK", "RANDOM_PORT", "DEFINED_PORT", "NONE");
    }

    @Test
    void springBootTest_shouldBeAnnotationType() {
        assertThat(SpringBootTest.class.isAnnotation())
                .as("@SpringBootTest must be an annotation type")
                .isTrue();
    }

    @Test
    void springBootTest_shouldDeclareClassesAttribute() throws NoSuchMethodException {
        assertThat(SpringBootTest.class.getDeclaredMethod("classes"))
                .as("@SpringBootTest must expose a 'classes' attribute for specifying "
                        + "the application configuration class")
                .isNotNull();
    }

    @Test
    void springBootTest_shouldDeclareWebEnvironmentAttribute() throws NoSuchMethodException {
        assertThat(SpringBootTest.class.getDeclaredMethod("webEnvironment"))
                .as("@SpringBootTest must expose a 'webEnvironment' attribute for specifying "
                        + "the web application type")
                .isNotNull();
    }

    @Test
    void springBootTest_shouldDeclarePropertiesAttribute() throws NoSuchMethodException {
        assertThat(SpringBootTest.class.getDeclaredMethod("properties"))
                .as("@SpringBootTest must expose a 'properties' attribute for overriding "
                        + "Spring environment properties in tests")
                .isNotNull();
    }

    @Test
    void springBootTest_shouldDeclareArgsAttribute() throws NoSuchMethodException {
        assertThat(SpringBootTest.class.getDeclaredMethod("args"))
                .as("@SpringBootTest must expose an 'args' attribute for passing "
                        + "program arguments, simulating bootRun --args behavior")
                .isNotNull();
    }
}
