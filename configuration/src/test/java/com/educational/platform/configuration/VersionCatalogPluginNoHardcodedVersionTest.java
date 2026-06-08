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
 * Guards against hardcoding plugin versions directly in the version catalog
 * instead of using {@code version.ref}. The project's versioning strategy
 * requires all plugin entries in {@code gradle/libs.versions.toml} to reference
 * a version key from the {@code [versions]} section via {@code version.ref}.
 * Hardcoding a version (e.g., {@code version = "4.0.1"}) would break the
 * unification pattern where the Spring Boot BOM and plugin share the same
 * version key ({@code spring}), and would make version upgrades error-prone.
 * <p>
 * Complements {@link VersionCatalogSpringVersionUnificationTest} (which
 * validates the shared {@code spring} key) and
 * {@link VersionCatalogPluginEntrySyntaxTest} (which validates inline-table
 * format). This test specifically guards against the {@code version = "..."}
 * pattern that would bypass version unification.
 */
class VersionCatalogPluginNoHardcodedVersionTest {

    private static String pluginsSection;
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
        String tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx)
                .as("[plugins] section must be present")
                .isGreaterThanOrEqualTo(0);
        pluginsSection = tomlContent.substring(pluginsIdx + "[plugins]".length());
        // Trim at next section header if present
        int nextSection = pluginsSection.indexOf("[");
        if (nextSection >= 0) {
            pluginsSection = pluginsSection.substring(0, nextSection);
        }
        pluginEntryLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
    }

    @Test
    void pluginEntries_shouldNotUseHardcodedVersion() {
        Pattern hardcodedVersionPattern = Pattern.compile(
                "version\\s*=\\s*\"[^\"]+\"");
        for (String line : pluginEntryLines) {
            Matcher matcher = hardcodedVersionPattern.matcher(line);
            if (matcher.find()) {
                // Only flag it if there's no version.ref in the same line
                assertThat(line)
                        .as("Plugin entry must use version.ref instead of hardcoded version: %s", line)
                        .contains("version.ref");
            }
        }
    }

    @Test
    void allPluginEntries_shouldHaveVersionRef() {
        Pattern versionRefPattern = Pattern.compile("version\\.ref\\s*=\\s*\"\\w+\"");
        for (String line : pluginEntryLines) {
            assertThat(line)
                    .as("Every plugin entry must include version.ref for version unification: %s", line)
                    .matches(".*" + versionRefPattern.pattern() + ".*");
        }
    }

    @Test
    void springbootPlugin_shouldReferenceSpringVersionKey() {
        String springbootLine = pluginEntryLines.stream()
                .filter(line -> line.startsWith("springboot"))
                .findFirst()
                .orElse(null);
        assertThat(springbootLine)
                .as("springboot plugin entry must exist in [plugins] section")
                .isNotNull();
        assertThat(springbootLine)
                .as("springboot plugin must use version.ref = \"spring\" to share the "
                        + "version key with the Spring BOM — hardcoding the version would "
                        + "create a version drift risk between plugin and BOM")
                .contains("version.ref = \"spring\"");
    }

    @Test
    void pluginEntries_shouldNotUsePlainVersionAttribute() {
        // version = "x.y.z" (without .ref) is the pattern we want to prevent
        Pattern plainVersion = Pattern.compile("\\bversion\\s*=\\s*\"\\d");
        for (String line : pluginEntryLines) {
            assertThat(line)
                    .as("Plugin entry must NOT use plain 'version = \"...\"' — "
                            + "use 'version.ref' instead: %s", line)
                    .doesNotMatch(".*" + plainVersion.pattern() + ".*");
        }
    }
}
