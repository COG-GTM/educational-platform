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
 * Validates the exact plugin IDs declared in the version catalog and their
 * correspondence with the build file references. The PR added the
 * springboot plugin entry; a typo in the plugin ID (e.g., missing a dot,
 * wrong casing, extra hyphen) would cause a Gradle resolution failure
 * at configuration time with an opaque "plugin not found" error.
 * <p>
 * Complements {@link VersionCatalogPluginCountInvariantTest} (entry count)
 * and {@link VersionCatalogStructuralIntegrityTest} (TOML structure).
 */
class VersionCatalogPluginIdValidationTest {

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
    void springbootPlugin_shouldHaveExactPluginId() {
        Matcher m = Pattern.compile("springboot.*id\\s*=\\s*\"([^\"]+)\"").matcher(tomlContent);
        assertThat(m.find())
                .as("springboot plugin entry must declare an id")
                .isTrue();
        assertThat(m.group(1))
                .as("springboot plugin ID must be exactly 'org.springframework.boot' — "
                        + "any deviation (typo, wrong casing) causes Gradle plugin resolution failure")
                .isEqualTo("org.springframework.boot");
    }

    @Test
    void springdependenciesPlugin_shouldHaveExactPluginId() {
        Matcher m = Pattern.compile("springdependencies.*id\\s*=\\s*\"([^\"]+)\"").matcher(tomlContent);
        assertThat(m.find())
                .as("springdependencies plugin entry must declare an id")
                .isTrue();
        assertThat(m.group(1))
                .as("springdependencies plugin ID must be exactly 'io.spring.dependency-management'")
                .isEqualTo("io.spring.dependency-management");
    }

    @Test
    void springbootPlugin_versionRef_shouldPointToSpringKey() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);

        Matcher m = Pattern.compile("springboot.*version\\.ref\\s*=\\s*\"(\\w+)\"")
                .matcher(pluginsSection);
        assertThat(m.find())
                .as("springboot plugin must use version.ref")
                .isTrue();
        assertThat(m.group(1))
                .as("springboot plugin version.ref must point to 'spring' key — "
                        + "the same key used by the root build's BOM import")
                .isEqualTo("spring");
    }

    @Test
    void springdependenciesPlugin_versionRef_shouldNotPointToSpringKey() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);

        Matcher m = Pattern.compile("springdependencies.*version\\.ref\\s*=\\s*\"(\\w+)\"")
                .matcher(pluginsSection);
        assertThat(m.find())
                .as("springdependencies plugin must use version.ref")
                .isTrue();
        assertThat(m.group(1))
                .as("springdependencies plugin must use its own version ref, not 'spring' — "
                        + "the dependency-management plugin has independent versioning "
                        + "from the Spring Boot plugin")
                .isNotEqualTo("spring");
    }

    @Test
    void pluginIds_shouldNotContainTrailingWhitespace() {
        Pattern idPattern = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
        Matcher m = idPattern.matcher(tomlContent);
        while (m.find()) {
            String id = m.group(1);
            assertThat(id)
                    .as("Plugin ID '%s' must not have leading or trailing whitespace", id)
                    .isEqualTo(id.trim());
        }
    }
}
