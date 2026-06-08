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
 * Guards the complete set of {@code implementation} dependencies in
 * {@code configuration/build.gradle.kts} against accidental removal.
 * <p>
 * {@link ConfigurationBuildTestDependencyCompletenessTest} guards
 * the <em>test</em> dependency inventory. This test guards the
 * <em>implementation</em> dependency inventory. The configuration module
 * aggregates all bounded-context modules plus framework libraries;
 * removing any project dependency silently breaks the monolith assembly,
 * while removing a library dependency causes runtime {@code ClassNotFoundException}.
 * <p>
 * The PR added three new <em>test</em> dependencies but did not change
 * implementation dependencies. This test ensures those implementation
 * dependencies remain intact after the merge.
 */
public class ConfigurationBuildImplementationDependencyGuardTest {

    private static final List<String> EXPECTED_PROJECT_DEPS = List.of(
            ":users:users-application",
            ":users:users-web",
            ":users:users-integration-events",
            ":administration:administration-application",
            ":administration:administration-web",
            ":administration:administration-integration-events",
            ":course-enrollments:course-enrollments-application",
            ":course-enrollments:course-enrollments-web",
            ":course-enrollments:course-enrollments-integration-events",
            ":course-reviews:course-reviews-application",
            ":course-reviews:course-reviews-web",
            ":course-reviews:course-reviews-integration-events",
            ":courses:courses-application",
            ":courses:courses-web",
            ":courses:courses-integration-events",
            ":security:security-config",
            ":web",
            ":common"
    );

    private static final List<String> EXPECTED_LIBRARY_DEPS = List.of(
            "spring-boot-starter-web",
            "liquibase-core"
    );

    @ParameterizedTest(name = "Project dependency should be declared: {0}")
    @ValueSource(strings = {
            ":users:users-application",
            ":users:users-web",
            ":users:users-integration-events",
            ":administration:administration-application",
            ":administration:administration-web",
            ":administration:administration-integration-events",
            ":course-enrollments:course-enrollments-application",
            ":course-enrollments:course-enrollments-web",
            ":course-enrollments:course-enrollments-integration-events",
            ":course-reviews:course-reviews-application",
            ":course-reviews:course-reviews-web",
            ":course-reviews:course-reviews-integration-events",
            ":courses:courses-application",
            ":courses:courses-web",
            ":courses:courses-integration-events",
            ":security:security-config",
            ":web",
            ":common"
    })
    void implementationProjectDep_shouldBeDeclared(String projectPath) throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration/build.gradle.kts should declare implementation(project(\"%s\"))", projectPath)
                .contains("implementation(project(\"" + projectPath + "\"))");
    }

    @ParameterizedTest(name = "Library dependency should be declared: {0}")
    @ValueSource(strings = {"spring-boot-starter-web", "liquibase-core"})
    void implementationLibraryDep_shouldBeDeclared(String artifactId) throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration/build.gradle.kts should declare implementation for '%s'", artifactId)
                .contains(artifactId);
    }

    @Test
    void implementationDependencyCount_shouldMatchExpected() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        long implDepCount = lines.stream()
                .map(String::trim)
                .filter(l -> l.startsWith("implementation(") && !l.startsWith("//"))
                .count();

        int expectedTotal = EXPECTED_PROJECT_DEPS.size() + EXPECTED_LIBRARY_DEPS.size();
        assertThat(implDepCount)
                .as("Total implementation dependency declarations should be %d (%d project + %d library)",
                        expectedTotal, EXPECTED_PROJECT_DEPS.size(), EXPECTED_LIBRARY_DEPS.size())
                .isEqualTo(expectedTotal);
    }

    @Test
    void noImplementationDep_shouldBeDuplicated() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        List<String> implLines = lines.stream()
                .map(String::trim)
                .filter(l -> l.startsWith("implementation(") && !l.startsWith("//"))
                .toList();

        assertThat(implLines)
                .as("No implementation dependency should appear more than once")
                .doesNotHaveDuplicates();
    }

    @Test
    void allBoundedContexts_shouldBeIncluded() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("All bounded contexts should have application, web, and integration-events modules")
                .contains(":users:users-application")
                .contains(":users:users-web")
                .contains(":users:users-integration-events")
                .contains(":administration:administration-application")
                .contains(":administration:administration-web")
                .contains(":administration:administration-integration-events")
                .contains(":course-enrollments:course-enrollments-application")
                .contains(":course-enrollments:course-enrollments-web")
                .contains(":course-enrollments:course-enrollments-integration-events")
                .contains(":course-reviews:course-reviews-application")
                .contains(":course-reviews:course-reviews-web")
                .contains(":course-reviews:course-reviews-integration-events")
                .contains(":courses:courses-application")
                .contains(":courses:courses-web")
                .contains(":courses:courses-integration-events");
    }

    @Test
    void crossCuttingModules_shouldBeIncluded() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Cross-cutting modules should be declared as implementation dependencies")
                .contains(":security:security-config")
                .contains(":web")
                .contains(":common");
    }

    @Test
    void implementationDeps_shouldNotInclude_testOnlyLibraries() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        List<String> implLines = lines.stream()
                .map(String::trim)
                .filter(l -> l.startsWith("implementation(") && !l.startsWith("//"))
                .toList();

        for (String line : implLines) {
            assertThat(line)
                    .as("implementation scope should not contain test-only libraries")
                    .doesNotContain("junit")
                    .doesNotContain("mockito")
                    .doesNotContain("assertj")
                    .doesNotContain("archunit");
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
