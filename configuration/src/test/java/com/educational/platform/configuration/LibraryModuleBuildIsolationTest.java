package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the architectural invariant that ONLY the configuration module applies
 * the Spring Boot plugin. Library modules must NOT apply it, as this would:
 * - Cause ambiguous main-class detection by the plugin (multiple @SpringBootApplication candidates)
 * - Generate unwanted fat JARs for library modules
 * - Break bootRun/bootJar task resolution in the configuration module
 */
class LibraryModuleBuildIsolationTest {

    private static Path projectRoot;

    @BeforeAll
    static void findProjectRoot() {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "courses/application/build.gradle.kts",
            "courses/web/build.gradle.kts",
            "courses/integration-events/build.gradle.kts",
            "administration/application/build.gradle.kts",
            "administration/web/build.gradle.kts",
            "administration/integration-events/build.gradle.kts",
            "course-enrollments/application/build.gradle.kts",
            "course-enrollments/web/build.gradle.kts",
            "course-enrollments/integration-events/build.gradle.kts",
            "course-reviews/application/build.gradle.kts",
            "course-reviews/web/build.gradle.kts",
            "course-reviews/integration-events/build.gradle.kts",
            "users/application/build.gradle.kts",
            "users/web/build.gradle.kts",
            "users/integration-events/build.gradle.kts",
            "common/build.gradle.kts",
            "web/build.gradle.kts"
    })
    void libraryModule_shouldNotApplySpringBootPlugin(String buildFilePath) throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return; // Module may not exist yet; skip gracefully
        }
        String content = Files.readString(buildFile);
        assertThat(content)
                .as("Module '%s' must NOT apply the Spring Boot plugin via alias — "
                        + "only the configuration module should apply it for single main-class detection",
                        buildFilePath)
                .doesNotContain("libs.plugins.springboot");
        assertThat(content)
                .as("Module '%s' must NOT apply the Spring Boot plugin via legacy apply syntax",
                        buildFilePath)
                .doesNotContainPattern("apply.*plugin.*org\\.springframework\\.boot");
    }

    @Test
    void settingsGradle_shouldIncludeConfigurationModule() throws IOException {
        String content = Files.readString(projectRoot.resolve("settings.gradle.kts"));
        assertThat(content)
                .as("settings.gradle.kts must include the configuration module")
                .contains("configuration");
    }

    @Test
    void onlyConfigurationModule_shouldHavePluginsBlock_withSpringBoot() throws IOException {
        String configContent = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(configContent)
                .as("configuration/build.gradle.kts must be the sole module applying springboot plugin")
                .contains("libs.plugins.springboot");
    }
}
