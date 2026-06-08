package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the test infrastructure enabled by the spring-boot-starter-test
 * dependency added in the PR. Beyond classpath availability (covered by
 * SpringBootStarterTestValidationTest), these tests verify the Gradle test
 * task configuration and transitive dependencies that starter-test brings
 * (Hamcrest, JSONassert, JsonPath) which are commonly used in integration tests.
 */
class TestInfrastructureConfigurationTest {

    private static String buildContent;

    @BeforeAll
    static void loadBuildFile() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        buildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void testTask_shouldUseJUnitPlatform() {
        assertThat(buildContent)
                .as("Test task must be configured with useJUnitPlatform() for JUnit 5 test discovery")
                .contains("useJUnitPlatform()");
    }

    @Test
    void testTask_shouldHaveTestBlock() {
        assertThat(buildContent)
                .as("Build file must contain a tasks.test configuration block")
                .containsPattern("tasks\\.test\\s*\\{");
    }

    @Test
    void hamcrestMatchers_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.hamcrest.Matchers"))
                .as("Hamcrest Matchers must be available via spring-boot-starter-test transitive dependency")
                .doesNotThrowAnyException();
    }

    @Test
    void hamcrestMatcherAssert_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.hamcrest.MatcherAssert"))
                .as("Hamcrest MatcherAssert must be available via spring-boot-starter-test transitive dependency")
                .doesNotThrowAnyException();
    }

    @Test
    void jsonAssert_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("org.skyscreamer.jsonassert.JSONAssert"))
                .as("JSONassert must be available via spring-boot-starter-test for JSON response validation")
                .doesNotThrowAnyException();
    }

    @Test
    void jsonPath_shouldBeOnTestClasspath() {
        assertThatCode(() -> Class.forName("com.jayway.jsonpath.JsonPath"))
                .as("JsonPath must be available via spring-boot-starter-test for JSON path assertions")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTestContextBootstrapper_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.context.SpringBootTestContextBootstrapper"))
                .as("SpringBootTestContextBootstrapper must be available for @SpringBootTest context loading")
                .doesNotThrowAnyException();
    }

    @Test
    void springTestWebServletMockMvc_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.web.servlet.MockMvc"))
                .as("MockMvc must be available via spring-boot-starter-test for controller testing")
                .doesNotThrowAnyException();
    }
}
