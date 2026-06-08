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
 * Validates the exact plugin set in gradle/libs.versions.toml and guards
 * against accidental addition of plugins that would conflict with the
 * Spring Boot plugin or the modular monolith architecture. Each plugin
 * added to the version catalog becomes available to every module via
 * {@code alias(libs.plugins.xxx)} — an accidental entry (e.g. a shadow
 * plugin, application plugin, or native-image plugin) could be applied
 * in a library module and produce conflicting fat JARs or task registrations.
 */
class VersionCatalogPluginCountInvariantTest {

    private static String tomlContent;
    private static List<String> pluginEntryLines;

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

        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx + "[plugins]".length());
        pluginEntryLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("["))
                .toList();
    }

    @Test
    void pluginEntries_shouldBeExactlySpringdependenciesAndSpringboot() {
        List<String> pluginNames = pluginEntryLines.stream()
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
        assertThat(pluginNames)
                .as("Version catalog must declare exactly springdependencies and springboot — "
                        + "additional plugins risk conflicting with the Spring Boot plugin's "
                        + "bootRun/bootJar task registration or BOM management")
                .containsExactlyInAnyOrder("springdependencies", "springboot");
    }

    @Test
    void allPlugins_shouldUseVersionRef_notInlineVersion() {
        for (String line : pluginEntryLines) {
            assertThat(line)
                    .as("Plugin '%s' must use version.ref for centralized version management — "
                            + "inline versions bypass the [versions] section", line)
                    .contains("version.ref");
        }
    }

    @Test
    void allPlugins_shouldHaveValidPluginId() {
        Pattern idPattern = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
        for (String line : pluginEntryLines) {
            Matcher m = idPattern.matcher(line);
            assertThat(m.find())
                    .as("Plugin entry must declare an id: %s", line)
                    .isTrue();
            String pluginId = m.group(1);
            assertThat(pluginId)
                    .as("Plugin id must follow reverse-domain convention: %s", pluginId)
                    .containsPattern("^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$");
        }
    }

    @Test
    void noPluginEntry_shouldUseShadowPlugin() {
        for (String line : pluginEntryLines) {
            assertThat(line)
                    .as("Shadow plugin must NOT be in the version catalog — "
                            + "the Spring Boot plugin handles fat JAR creation via bootJar")
                    .doesNotContain("com.github.johnrengelman.shadow")
                    .doesNotContain("shadow");
        }
    }

    @Test
    void noPluginEntry_shouldUseNativeImagePlugin() {
        for (String line : pluginEntryLines) {
            assertThat(line)
                    .as("GraalVM native-image plugin must NOT be in the version catalog — "
                            + "native compilation is not part of the current architecture")
                    .doesNotContain("org.graalvm")
                    .doesNotContain("native");
        }
    }
}
