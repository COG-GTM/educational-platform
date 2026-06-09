package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that settings.gradle.kts does not interfere with the version catalog
 * plugin resolution mechanism. Gradle 7.4+ auto-discovers the version catalog at
 * the conventional path {@code gradle/libs.versions.toml}. If settings.gradle.kts
 * overrides the catalog location, renames it, or adds pluginManagement blocks with
 * hardcoded versions, the {@code alias(libs.plugins.springboot)} resolution in the
 * configuration module would break or resolve an unexpected version.
 */
class SettingsGradlePluginResolutionTest {

    private static String settingsContent;
    private static Path projectRoot;

    @BeforeAll
    static void loadSettings() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        settingsContent = Files.readString(projectRoot.resolve("settings.gradle.kts"));
    }

    @Test
    void settings_shouldNotOverrideVersionCatalogLocation() {
        assertThat(settingsContent)
                .as("settings.gradle.kts must not override the default version catalog location — "
                        + "Gradle auto-discovers gradle/libs.versions.toml by convention")
                .doesNotContainPattern("versionCatalogs\\s*\\{");
    }

    @Test
    void settings_shouldNotDeclarePluginManagementWithHardcodedVersions() {
        assertThat(settingsContent)
                .as("settings.gradle.kts must not use pluginManagement with hardcoded plugin versions — "
                        + "versions should come from the version catalog for single-source-of-truth")
                .doesNotContainPattern("pluginManagement\\s*\\{");
    }

    @Test
    void settings_shouldNotExcludeConfigurationModule() {
        assertThat(settingsContent)
                .as("settings.gradle.kts must include the configuration module for bootRun to work")
                .contains("include(\"configuration\")");
    }

    @Test
    void versionCatalogFile_shouldExistAtConventionalPath() {
        assertThat(Files.exists(projectRoot.resolve("gradle/libs.versions.toml")))
                .as("gradle/libs.versions.toml must exist at the conventional path for Gradle auto-discovery")
                .isTrue();
    }

    @Test
    void settings_shouldNotRenameConfigurationProject() {
        assertThat(settingsContent.lines()
                .filter(line -> line.contains("findProject") && line.contains("configuration"))
                .toList())
                .as("settings.gradle.kts must not rename the configuration project — "
                        + "the :configuration task path must remain valid for ./gradlew :configuration:bootRun")
                .isEmpty();
    }

    @Test
    void settings_shouldNotDeclareResolutionStrategyForSpringBoot() {
        assertThat(settingsContent)
                .as("settings.gradle.kts must not use resolutionStrategy for Spring Boot plugin — "
                        + "version should be managed exclusively by the version catalog")
                .doesNotContain("resolutionStrategy");
    }

    @Test
    void settings_shouldUseKotlinDsl() {
        assertThat(Files.exists(projectRoot.resolve("settings.gradle.kts")))
                .as("settings must use Kotlin DSL (.kts) for consistency with build.gradle.kts files")
                .isTrue();
        assertThat(Files.exists(projectRoot.resolve("settings.gradle")))
                .as("Groovy settings.gradle must NOT exist alongside Kotlin settings.gradle.kts")
                .isFalse();
    }
}
