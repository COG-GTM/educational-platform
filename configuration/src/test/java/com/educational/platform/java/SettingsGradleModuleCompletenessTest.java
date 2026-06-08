package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that settings.gradle.kts includes all expected bounded context modules.
 * <p>
 * After the Java 26 upgrade, all modules must be compiled with the new version.
 * If a module is accidentally removed from settings.gradle.kts, Gradle will not
 * compile it — meaning that module's bytecode stays at the old Java version or
 * is simply missing from the build. This test guards against such omissions.
 */
public class SettingsGradleModuleCompletenessTest {

    @ParameterizedTest(name = "settings.gradle.kts should include module: {0}")
    @ValueSource(strings = {
            "configuration",
            "common",
            "web",
            "courses:application",
            "courses:web",
            "courses:integration-events",
            "administration:application",
            "administration:web",
            "administration:integration-events",
            "course-enrollments:application",
            "course-enrollments:web",
            "course-enrollments:integration-events",
            "course-reviews:application",
            "course-reviews:web",
            "course-reviews:integration-events",
            "users:application",
            "users:web",
            "users:integration-events",
            "security:config",
            "security:test"
    })
    void settingsGradle_shouldInclude_module(String modulePath) throws IOException {
        String content = readSettingsGradle();

        // settings.gradle.kts uses include("module:submodule") format
        String includePattern = "\"" + modulePath + "\"";
        assertThat(content)
                .as("settings.gradle.kts should include module: %s", modulePath)
                .contains(includePattern);
    }

    @Test
    void settingsGradle_shouldDefine_rootProjectName() throws IOException {
        String content = readSettingsGradle();

        assertThat(content)
                .as("settings.gradle.kts should define rootProject.name")
                .containsPattern("rootProject\\.name\\s*=");
    }

    @Test
    void settingsGradle_shouldHave_atLeast20Modules() throws IOException {
        String content = readSettingsGradle();
        long includeCount = content.lines()
                .filter(line -> line.trim().startsWith("include("))
                .count();

        assertThat(includeCount)
                .as("settings.gradle.kts should include at least 20 modules")
                .isGreaterThanOrEqualTo(20);
    }

    @Test
    void settingsGradle_shouldNotContain_commentedOutModules() throws IOException {
        String content = readSettingsGradle();
        long commentedIncludes = content.lines()
                .filter(line -> line.trim().startsWith("//") && line.contains("include("))
                .count();

        assertThat(commentedIncludes)
                .as("settings.gradle.kts should not have commented-out include statements")
                .isZero();
    }

    @Test
    void settingsGradle_shouldNotReference_oldVersions() throws IOException {
        String content = readSettingsGradle();

        assertThat(content)
                .as("settings.gradle.kts should not reference old Java or Gradle versions")
                .doesNotContain("Java 25")
                .doesNotContain("java 25")
                .doesNotContain("9.2.1");
    }

    @Test
    void eachIncludedModule_shouldHave_buildFile() throws IOException {
        String content = readSettingsGradle();
        Path root = findProjectRoot();

        content.lines()
                .filter(line -> line.trim().startsWith("include("))
                .map(line -> line.replaceAll(".*\"([^\"]+)\".*", "$1"))
                .map(module -> module.replace(":", "/"))
                .forEach(modulePath -> {
                    Path buildFile = root.resolve(modulePath).resolve("build.gradle.kts");
                    assertThat(buildFile)
                            .as("Module '%s' should have a build.gradle.kts", modulePath)
                            .exists();
                });
    }

    @Test
    void allBoundedContexts_shouldHave_applicationAndWebModules() throws IOException {
        String content = readSettingsGradle();

        for (String context : new String[]{"courses", "administration", "course-enrollments",
                "course-reviews", "users"}) {
            assertThat(content)
                    .as("Bounded context '%s' should have :application submodule", context)
                    .contains("\"" + context + ":application\"");

            assertThat(content)
                    .as("Bounded context '%s' should have :web submodule", context)
                    .contains("\"" + context + ":web\"");

            assertThat(content)
                    .as("Bounded context '%s' should have :integration-events submodule", context)
                    .contains("\"" + context + ":integration-events\"");
        }
    }

    private String readSettingsGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("settings.gradle.kts"));
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
