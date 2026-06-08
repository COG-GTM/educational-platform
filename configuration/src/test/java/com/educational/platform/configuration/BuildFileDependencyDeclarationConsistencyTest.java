package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the declaration style consistency of dependencies in
 * configuration/build.gradle.kts. The PR added spring-boot-starter-test
 * using two-argument (group, artifact) notation without an explicit version,
 * matching the existing convention for spring-boot-starter-web. These tests
 * guard against notation drift that would make the build file harder to
 * maintain and could accidentally bypass BOM version management.
 */
class BuildFileDependencyDeclarationConsistencyTest {

    private static String buildContent;
    private static List<String> allDepLines;

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
        allDepLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("implementation(") || line.startsWith("testImplementation("))
                .toList();
    }

    @Test
    void springBootStarters_shouldUseConsistentTwoArgNotation() {
        List<String> starterLines = allDepLines.stream()
                .filter(line -> line.contains("spring-boot-starter"))
                .toList();
        assertThat(starterLines).isNotEmpty();
        for (String line : starterLines) {
            assertThat(line)
                    .as("Spring Boot starter '%s' must use two-argument (group, artifact) notation "
                            + "matching the project convention", line)
                    .containsPattern("\\(\\s*\"org\\.springframework\\.boot\"\\s*,\\s*\"spring-boot-starter");
        }
    }

    @Test
    void springBootStarters_shouldNotDeclareExplicitVersions() {
        List<String> starterLines = allDepLines.stream()
                .filter(line -> line.contains("spring-boot-starter"))
                .toList();
        for (String line : starterLines) {
            long commaCount = line.chars().filter(c -> c == ',').count();
            assertThat(commaCount)
                    .as("Spring Boot starter dependency must have exactly one comma (group, artifact) — "
                            + "a third argument would be an explicit version bypassing BOM management: %s", line)
                    .isEqualTo(1);
        }
    }

    @Test
    void externalDepsWithExplicitVersion_shouldUseLibsVersionsGet() {
        List<String> threeArgLines = allDepLines.stream()
                .filter(line -> !line.contains("project("))
                .filter(line -> line.chars().filter(c -> c == ',').count() >= 2)
                .toList();
        for (String line : threeArgLines) {
            assertThat(line)
                    .as("Dependencies with explicit versions must reference the version catalog "
                            + "via libs.versions.<key>.get() for centralized management: %s", line)
                    .contains("libs.versions.");
        }
    }

    @Test
    void projectDependencies_shouldUseProjectNotation() {
        List<String> moduleDeps = allDepLines.stream()
                .filter(line -> line.contains("project("))
                .toList();
        for (String line : moduleDeps) {
            assertThat(line)
                    .as("Module dependencies must use project(\":module:path\") notation: %s", line)
                    .containsPattern("project\\(\":[^\"]+\"\\)");
        }
    }

    @Test
    void noDependency_shouldUseStringNotation() {
        for (String line : allDepLines) {
            assertThat(line)
                    .as("Dependencies must not use single-string GAV notation (\"group:artifact:version\") — "
                            + "use two/three-arg or project() notation: %s", line)
                    .doesNotMatch(".*\\(\\s*\"[^\"]+:[^\"]+:[^\"]+\"\\s*\\).*");
        }
    }

    @Test
    void allExternalDeps_shouldUseDoubleQuotesForCoordinates() {
        for (String line : allDepLines) {
            if (!line.contains("project(")) {
                assertThat(line)
                        .as("External dependency coordinates must use double quotes: %s", line)
                        .doesNotContain("'");
            }
        }
    }
}
