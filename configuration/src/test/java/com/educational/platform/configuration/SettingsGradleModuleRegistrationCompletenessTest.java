package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the module registration in settings.gradle.kts is complete and
 * consistent with the configuration module's dependency declarations. The
 * Spring Boot plugin's bootRun/bootJar tasks package all {@code implementation}
 * dependencies into the executable artifact. If a module is declared as an
 * implementation dependency in configuration/build.gradle.kts but not
 * registered in settings.gradle.kts, Gradle fails with "project not found".
 * Conversely, if a module is registered in settings but not referenced by the
 * configuration module, it will not be included in the bootJar.
 * <p>
 * Complements {@link ConfigurationModuleImplementationDepsCompletenessTest}
 * which validates dependency completeness, and
 * {@link SettingsGradlePluginResolutionTest} which validates plugin
 * resolution configuration.
 */
class SettingsGradleModuleRegistrationCompletenessTest {

    private static String settingsContent;
    private static String configBuildContent;

    @BeforeAll
    static void loadFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        settingsContent = Files.readString(dir.resolve("settings.gradle.kts"));
        configBuildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void allConfigurationModuleDeps_shouldBeRegisteredInSettings() {
        Pattern projectRefPattern = Pattern.compile(
                "implementation\\s*\\(\\s*project\\s*\\(\\s*\"([^\"]+)\"");
        Matcher matcher = projectRefPattern.matcher(configBuildContent);

        while (matcher.find()) {
            String projectPath = matcher.group(1);
            // Convert Gradle path ":courses:courses-web" to settings include
            // format "courses:web" or "courses:courses-web"
            String[] parts = projectPath.split(":");
            // The last part after the last colon should be mentioned in settings
            String lastPart = parts[parts.length - 1];
            assertThat(settingsContent)
                    .as("Module '%s' is declared as an implementation dependency in "
                            + "configuration/build.gradle.kts but must also be registered "
                            + "in settings.gradle.kts for Gradle to resolve it", projectPath)
                    .contains(lastPart);
        }
    }

    @Test
    void settings_shouldDeclareAllFiveBoundedContexts() {
        List<String> boundedContexts = List.of(
                "courses", "administration", "course-enrollments",
                "course-reviews", "users"
        );
        for (String ctx : boundedContexts) {
            assertThat(settingsContent)
                    .as("Bounded context '%s' must be registered in settings.gradle.kts — "
                            + "bootRun requires all modules to be included in the build", ctx)
                    .contains(ctx);
        }
    }

    @Test
    void settings_shouldDeclareSecurityModules() {
        assertThat(settingsContent)
                .as("security:config must be registered in settings.gradle.kts — "
                        + "Spring Security auto-configuration depends on it")
                .contains("security:config");
        assertThat(settingsContent)
                .as("security:test must be registered for test utilities")
                .contains("security:test");
    }

    @Test
    void settings_shouldDeclareInfrastructureModules() {
        assertThat(settingsContent)
                .as("'common' module must be registered")
                .containsPattern("include\\s*\\(\\s*\"common\"\\s*\\)");
        assertThat(settingsContent)
                .as("'web' module must be registered")
                .containsPattern("include\\s*\\(\\s*\"web\"\\s*\\)");
        assertThat(settingsContent)
                .as("'configuration' module must be registered for bootRun")
                .containsPattern("include\\s*\\(\\s*\"configuration\"\\s*\\)");
    }

    @Test
    void settings_rootProjectName_shouldBePlatform() {
        assertThat(settingsContent)
                .as("Root project name should be 'platform' — "
                        + "this name is used as the default prefix for bootJar artifact naming")
                .containsPattern("rootProject\\.name\\s*=\\s*\"platform\"");
    }

    @Test
    void eachBoundedContext_shouldRegisterThreeSubmodules() {
        List<String> contexts = List.of(
                "courses", "administration", "course-enrollments",
                "course-reviews", "users"
        );
        for (String ctx : contexts) {
            long includeCount = settingsContent.lines()
                    .filter(line -> line.contains("include(") && line.contains(ctx + ":"))
                    .count();
            assertThat(includeCount)
                    .as("Bounded context '%s' must register exactly 3 submodules "
                            + "(application, web, integration-events) — bootRun needs all "
                            + "three on the classpath", ctx)
                    .isEqualTo(3);
        }
    }
}
