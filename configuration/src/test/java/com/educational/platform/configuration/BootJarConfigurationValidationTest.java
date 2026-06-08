package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin configuration in the build files
 * supports bootJar and bootRun correctly. Guards against accidental
 * disabling of boot tasks or misapplication of the plugin.
 */
class BootJarConfigurationValidationTest {

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

    @Test
    void configurationBuildGradle_shouldNotDisableBootJar() throws IOException {
        String content = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(content)
                .as("bootJar task must not be explicitly disabled — the configuration module "
                        + "is the application entry point and must produce an executable JAR")
                .doesNotContainPattern("bootJar.*enabled\\s*=\\s*false");
    }

    @Test
    void configurationBuildGradle_pluginsBlock_shouldAppearBeforeDependencies() throws IOException {
        String content = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        int pluginsIndex = content.indexOf("plugins");
        int dependenciesIndex = content.indexOf("dependencies");
        assertThat(pluginsIndex)
                .as("plugins block must exist in configuration/build.gradle.kts")
                .isGreaterThanOrEqualTo(0);
        assertThat(dependenciesIndex)
                .as("dependencies block must exist in configuration/build.gradle.kts")
                .isGreaterThanOrEqualTo(0);
        assertThat(pluginsIndex)
                .as("plugins block must appear before dependencies block (Gradle DSL requirement)")
                .isLessThan(dependenciesIndex);
    }

    @Test
    void rootBuildGradle_shouldNotApplySpringBootPlugin() throws IOException {
        String content = Files.readString(projectRoot.resolve("build.gradle.kts"));
        assertThat(content)
                .as("Root build.gradle.kts must NOT apply the Spring Boot plugin — "
                        + "only the configuration module should apply it")
                .doesNotContain("libs.plugins.springboot");
    }

    @Test
    void configurationBuildGradle_shouldNotUseLegacyPluginApplication() throws IOException {
        String content = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(content)
                .as("Spring Boot plugin must be applied via plugins DSL, not legacy apply syntax")
                .doesNotContainPattern("apply.*plugin.*org\\.springframework\\.boot");
    }
}
