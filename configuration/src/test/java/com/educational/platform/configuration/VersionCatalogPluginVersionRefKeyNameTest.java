package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the specific version.ref key names used by plugin entries in
 * gradle/libs.versions.toml. The springboot plugin must use version.ref = "spring"
 * because the root build.gradle.kts imports the Spring Boot BOM via
 * {@code libs.versions.spring.get()}. If the version.ref key name is changed
 * (e.g., to "springBoot" or "bootVersion") while keeping the value the same,
 * the plugin would still work but the root BOM import would break, causing
 * unmanaged transitive dependency versions across all subprojects.
 *
 * VersionCatalogCrossValidationTest validates that the version.ref resolves to
 * the correct runtime version value; this test guards the key NAME itself.
 */
class VersionCatalogPluginVersionRefKeyNameTest {

    private static String tomlContent;
    private static String rootBuildContent;

    @BeforeAll
    static void loadFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
        rootBuildContent = Files.readString(dir.resolve("build.gradle.kts"));
    }

    @Test
    void springbootPlugin_versionRefKey_shouldBeSpring() {
        String versionRefKey = extractVersionRefKey("springboot");
        assertThat(versionRefKey)
                .as("springboot plugin version.ref must be \"spring\" — this key is also "
                        + "referenced by the root build's BOM import (libs.versions.spring.get()). "
                        + "Changing the key name would break BOM version alignment across all subprojects.")
                .isEqualTo("spring");
    }

    @Test
    void springdependenciesPlugin_versionRefKey_shouldBeSpringDependencyManagementPlugin() {
        String versionRefKey = extractVersionRefKey("springdependencies");
        assertThat(versionRefKey)
                .as("springdependencies plugin version.ref must be \"springDependencyManagementPlugin\" — "
                        + "this plugin has its own independent version lifecycle separate from Spring Boot")
                .isEqualTo("springDependencyManagementPlugin");
    }

    @Test
    void rootBuild_bomImport_shouldReferenceSpringVersionKey() {
        assertThat(rootBuildContent)
                .as("Root build BOM import must use libs.versions.spring.get() — "
                        + "the 'spring' key must match the springboot plugin's version.ref")
                .contains("libs.versions.spring.get()");
    }

    @Test
    void springbootPlugin_andRootBom_shouldShareSameVersionKey() {
        String pluginVersionRefKey = extractVersionRefKey("springboot");

        // The root build references the version catalog as libs.versions.<key>.get()
        String expectedAccessor = "libs.versions." + pluginVersionRefKey + ".get()";
        assertThat(rootBuildContent)
                .as("Root build BOM import must reference the same version catalog key ('%s') "
                        + "as the springboot plugin's version.ref to prevent version skew",
                        pluginVersionRefKey)
                .contains(expectedAccessor);
    }

    @Test
    void versionRefKeys_shouldNotBeSameForBothPlugins() {
        String springbootRef = extractVersionRefKey("springboot");
        String springdepsRef = extractVersionRefKey("springdependencies");
        assertThat(springbootRef)
                .as("springboot and springdependencies must use different version.ref keys — "
                        + "they have independent version lifecycles (Spring Boot 4.0.1 vs "
                        + "dependency-management plugin 1.x)")
                .isNotEqualTo(springdepsRef);
    }

    private String extractVersionRefKey(String pluginName) {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);
        for (String line : pluginsSection.lines().toList()) {
            if (line.trim().startsWith(pluginName)) {
                Matcher m = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"").matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }
}
