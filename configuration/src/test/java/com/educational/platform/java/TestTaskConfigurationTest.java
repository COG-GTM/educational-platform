package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structural correctness of the {@code tasks.test} block in
 * {@code configuration/build.gradle.kts}.
 * <p>
 * The configuration module is the boot entry point that aggregates all
 * submodules. Its {@code tasks.test { useJUnitPlatform() }} block is critical:
 * without it, Gradle uses the default test runner which does not discover
 * JUnit Jupiter tests, causing all tests to silently pass with 0 tests run.
 * <p>
 * {@link ConfigurationModuleDependenciesTest} verifies the string
 * {@code useJUnitPlatform()} is present. This test verifies the
 * <em>structural</em> properties of the test task block:
 * <ul>
 *   <li>{@code tasks.test} block exists and is properly opened/closed</li>
 *   <li>{@code useJUnitPlatform()} appears inside the block, not standalone</li>
 *   <li>No conflicting test framework configurations exist</li>
 *   <li>The block appears after the dependencies block</li>
 * </ul>
 */
public class TestTaskConfigurationTest {

    @Test
    void configBuild_shouldHave_tasksTestBlock() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration build.gradle.kts should contain tasks.test block")
                .containsPattern("tasks\\.test\\s*\\{");
    }

    @Test
    void useJUnitPlatform_shouldAppearInside_tasksTestBlock() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        int tasksTestLine = -1;
        int useJUnitPlatformLine = -1;
        int closingBraceLine = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("tasks.test")) {
                tasksTestLine = i;
            }
            if (trimmed.contains("useJUnitPlatform()")) {
                useJUnitPlatformLine = i;
            }
        }

        assertThat(tasksTestLine)
                .as("tasks.test block should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(useJUnitPlatformLine)
                .as("useJUnitPlatform() should exist")
                .isGreaterThanOrEqualTo(0);

        // Find the closing brace after tasks.test
        int braceDepth = 0;
        for (int i = tasksTestLine; i < lines.size(); i++) {
            for (char c : lines.get(i).toCharArray()) {
                if (c == '{') braceDepth++;
                if (c == '}') braceDepth--;
            }
            if (braceDepth == 0 && i > tasksTestLine) {
                closingBraceLine = i;
                break;
            }
        }

        assertThat(closingBraceLine)
                .as("tasks.test block should have a matching closing brace")
                .isGreaterThan(tasksTestLine);

        assertThat(useJUnitPlatformLine)
                .as("useJUnitPlatform() should appear after tasks.test opening")
                .isGreaterThan(tasksTestLine)
                .as("useJUnitPlatform() should appear before tasks.test closing brace")
                .isLessThan(closingBraceLine);
    }

    @Test
    void tasksTestBlock_shouldAppearAfter_dependenciesBlock() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        int dependenciesBlockStart = -1;
        int tasksTestBlockStart = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("dependencies") && trimmed.contains("{")) {
                dependenciesBlockStart = i;
            }
            if (trimmed.startsWith("tasks.test")) {
                tasksTestBlockStart = i;
            }
        }

        assertThat(dependenciesBlockStart)
                .as("dependencies block should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(tasksTestBlockStart)
                .as("tasks.test block should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(tasksTestBlockStart)
                .as("tasks.test block should appear after dependencies block")
                .isGreaterThan(dependenciesBlockStart);
    }

    @Test
    void configBuild_shouldNotConfigure_conflictingTestFrameworks() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should not configure TestNG alongside JUnit Platform")
                .doesNotContain("useTestNG");

        assertThat(content)
                .as("Should not use old JUnit 4 runner")
                .doesNotContain("junit4");
    }

    @Test
    void configBuild_shouldHave_exactlyOne_useJUnitPlatformCall() throws IOException {
        List<String> lines = Files.readAllLines(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        long count = lines.stream()
                .map(String::trim)
                .filter(line -> line.contains("useJUnitPlatform()"))
                .count();

        assertThat(count)
                .as("There should be exactly one useJUnitPlatform() call")
                .isEqualTo(1);
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
