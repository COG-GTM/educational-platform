package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the completeness of test dependencies in
 * {@code configuration/build.gradle.kts} after the Java 26 upgrade.
 * <p>
 * The upgrade added three new test dependencies:
 * {@code junit-jupiter-params} (testImplementation),
 * {@code junit-jupiter-engine} (testRuntimeOnly), and
 * {@code assertj-core} (testImplementation). These additions increased
 * the total test dependency count from 5 to 8.
 * <p>
 * {@link TestDependencyScopeValidationTest} validates correct scopes.
 * {@link NewTestDependencyClasspathTest} validates classpath availability.
 * This test guards against accidental <em>removal</em> of any test dependency
 * (e.g., via a careless merge or IDE refactor) by asserting the exact set
 * of expected test dependencies.
 */
public class ConfigurationBuildTestDependencyCompletenessTest {

    private static final List<String> EXPECTED_TEST_DEPS = List.of(
            "junit-jupiter-api",
            "junit-jupiter-params",
            "junit-jupiter-engine",
            "junit-platform-engine",
            "junit-platform-launcher",
            "mockito-junit-jupiter",
            "assertj-core",
            "archunit-junit5"
    );

    @ParameterizedTest(name = "Test dependency ''{0}'' should be declared")
    @ValueSource(strings = {
            "junit-jupiter-api",
            "junit-jupiter-params",
            "junit-jupiter-engine",
            "junit-platform-engine",
            "junit-platform-launcher",
            "mockito-junit-jupiter",
            "assertj-core",
            "archunit-junit5"
    })
    void testDependency_shouldBeDeclared(String artifactId) throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration/build.gradle.kts should declare test dependency '%s'", artifactId)
                .contains(artifactId);
    }

    @Test
    void testDependencyCount_shouldMatchExpected() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        long testDepCount = lines.stream()
                .map(String::trim)
                .filter(l -> l.startsWith("testImplementation(") || l.startsWith("testRuntimeOnly("))
                .count();

        assertThat(testDepCount)
                .as("Total test dependency declarations should be %d (5 pre-existing + 3 new)",
                        EXPECTED_TEST_DEPS.size())
                .isEqualTo(EXPECTED_TEST_DEPS.size());
    }

    @Test
    void noTestDependency_shouldBeDuplicated() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        List<String> testDepLines = lines.stream()
                .map(String::trim)
                .filter(l -> l.startsWith("testImplementation(") || l.startsWith("testRuntimeOnly("))
                .toList();

        assertThat(testDepLines)
                .as("No test dependency should appear more than once")
                .doesNotHaveDuplicates();
    }

    @Test
    void newDependencies_shouldAllBePresent_together() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("All three new dependencies from the Java 26 upgrade should be present together")
                .contains("junit-jupiter-params")
                .contains("junit-jupiter-engine")
                .contains("assertj-core");
    }

    @Test
    void preExistingDependencies_shouldNotBeRemoved() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Pre-existing test dependencies should still be declared")
                .contains("junit-jupiter-api")
                .contains("junit-platform-engine")
                .contains("junit-platform-launcher")
                .contains("mockito-junit-jupiter")
                .contains("archunit-junit5");
    }

    @Test
    void implementationDependencies_shouldNotBeMixedIntoTestScope() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("testImplementation(") || trimmed.startsWith("testRuntimeOnly(")) {
                assertThat(trimmed)
                        .as("Test-scoped dependency should not reference a project() dependency: %s", trimmed)
                        .doesNotContain("project(");
            }
        }
    }

    private String readConfigBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path settingsFile = current.resolve("settings.gradle.kts");
            Path gradleDir = current.resolve("gradle/wrapper");
            if (Files.exists(settingsFile) || Files.isDirectory(gradleDir)) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
