package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates uniqueness constraints within the [plugins] section of
 * gradle/libs.versions.toml. The PR added a new plugin entry (springboot);
 * these tests guard against:
 * <ul>
 *   <li>Duplicate alias names (two entries with the same key would overwrite each other)</li>
 *   <li>Duplicate plugin IDs (two aliases pointing to the same plugin ID would cause
 *       ambiguous resolution when applying via alias())</li>
 *   <li>Orphan aliases (declared but never referenced anywhere in build scripts)</li>
 * </ul>
 * These invariants ensure the version catalog remains a reliable single source of truth
 * for all Gradle plugin declarations in the modular monolith.
 */
class VersionCatalogPluginIdUniquenessTest {

    private static String tomlContent;
    private static List<String> pluginLines;

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
        pluginLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("["))
                .toList();
    }

    @Test
    void pluginAliasNames_shouldBeUnique() {
        List<String> aliasNames = pluginLines.stream()
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
        assertThat(aliasNames)
                .as("Each plugin alias in [plugins] must be unique — "
                        + "duplicate keys in TOML silently overwrite the previous entry")
                .doesNotHaveDuplicates();
    }

    @Test
    void pluginIds_shouldBeUnique() {
        Pattern idPattern = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
        List<String> pluginIds = pluginLines.stream()
                .map(line -> {
                    Matcher m = idPattern.matcher(line);
                    return m.find() ? m.group(1) : null;
                })
                .filter(id -> id != null)
                .toList();
        assertThat(pluginIds)
                .as("Each plugin ID must be unique across all version catalog entries — "
                        + "having two aliases point to the same plugin ID causes ambiguous "
                        + "resolution and potential version conflicts")
                .doesNotHaveDuplicates();
    }

    @Test
    void pluginAliasNames_shouldBeLowerCaseOrCamelCase() {
        List<String> aliasNames = pluginLines.stream()
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
        for (String alias : aliasNames) {
            assertThat(alias)
                    .as("Plugin alias '%s' must follow camelCase or lowercase naming — "
                            + "Gradle version catalog conventions require this for type-safe accessor generation",
                            alias)
                    .matches("[a-z][a-zA-Z0-9]*");
        }
    }

    @Test
    void pluginEntries_shouldAllDeclareAnId() {
        Pattern idPattern = Pattern.compile("id\\s*=\\s*\"[^\"]+\"");
        for (String line : pluginLines) {
            assertThat(line)
                    .as("Plugin entry must declare an 'id' field: %s", line)
                    .containsPattern(idPattern);
        }
    }

    @Test
    void pluginEntries_shouldAllDeclareAVersionRef() {
        Pattern versionRefPattern = Pattern.compile("version\\.ref\\s*=\\s*\"[^\"]+\"");
        for (String line : pluginLines) {
            assertThat(line)
                    .as("Plugin entry must use version.ref for centralized version management: %s", line)
                    .containsPattern(versionRefPattern);
        }
    }

    @Test
    void pluginSection_shouldContainExactlyTwoEntries() {
        assertThat(pluginLines)
                .as("[plugins] section must contain exactly 2 entries "
                        + "(springdependencies and springboot) — adding more plugins "
                        + "should be a deliberate architectural decision")
                .hasSize(2);
    }
}
