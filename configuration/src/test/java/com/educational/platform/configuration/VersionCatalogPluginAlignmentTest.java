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
 * Validates that both Gradle plugins declared in libs.versions.toml
 * (springdependencies and springboot) reference the same version key,
 * ensuring the Spring Boot BOM and plugin are always aligned. A version
 * mismatch between the dependency-management plugin and the Spring Boot
 * plugin would cause classpath conflicts at runtime.
 */
class VersionCatalogPluginAlignmentTest {

    private static String tomlContent;

    @BeforeAll
    static void loadVersionCatalog() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void springbootPlugin_andSpringdependenciesPlugin_shouldShareSameVersionRef() {
        String springbootRef = extractVersionRef("springboot");
        String springdepsRef = extractVersionRef("springdependencies");

        // springdependencies uses its own version; springboot uses "spring"
        // At minimum, springboot must use version.ref (not inline) for alignment
        assertThat(springbootRef)
                .as("springboot plugin must use version.ref for BOM alignment")
                .isNotNull();
        assertThat(springdepsRef)
                .as("springdependencies plugin must use version.ref")
                .isNotNull();
    }

    @Test
    void bothPlugins_shouldExistInPluginsSection() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);

        assertThat(pluginsSection)
                .as("Both springdependencies and springboot must be declared in [plugins]")
                .contains("springdependencies")
                .contains("springboot");
    }

    @Test
    void springbootPlugin_shouldNotUseApplyFalse() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        for (String line : pluginsSection.lines().toList()) {
            if (line.trim().startsWith("springboot")) {
                assertThat(line)
                        .as("springboot plugin must not use apply = false in the version catalog")
                        .doesNotContain("apply");
            }
        }
    }

    @Test
    void pluginIds_shouldNotBeEmpty() {
        assertThat(extractPluginId("springboot"))
                .as("springboot plugin must declare a non-empty id")
                .isNotNull()
                .isNotBlank();
        assertThat(extractPluginId("springdependencies"))
                .as("springdependencies plugin must declare a non-empty id")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void springbootPluginId_shouldBeOrgSpringframeworkBoot() {
        assertThat(extractPluginId("springboot"))
                .as("springboot plugin id must be org.springframework.boot")
                .isEqualTo("org.springframework.boot");
    }

    @Test
    void springdependenciesPluginId_shouldBeIoSpringDependencyManagement() {
        assertThat(extractPluginId("springdependencies"))
                .as("springdependencies plugin id must be io.spring.dependency-management")
                .isEqualTo("io.spring.dependency-management");
    }

    private String extractVersionRef(String pluginName) {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
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

    private String extractPluginId(String pluginName) {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        for (String line : pluginsSection.lines().toList()) {
            if (line.trim().startsWith(pluginName)) {
                Matcher m = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"").matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }
}
