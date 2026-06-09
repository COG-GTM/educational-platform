package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the TOML syntax of plugin entries in gradle/libs.versions.toml.
 * The PR added the springboot plugin entry using inline table syntax:
 * {@code springboot = { id = "org.springframework.boot", version.ref = "spring" }}
 * These tests guard against common TOML format errors that would cause
 * Gradle to fail at configuration time with a cryptic parsing error.
 */
class VersionCatalogPluginEntrySyntaxTest {

    private static String tomlContent;
    private static List<String> tomlLines;

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
        tomlLines = tomlContent.lines().toList();
    }

    @Test
    void springbootPlugin_shouldUseInlineTableSyntax() {
        String springbootLine = tomlLines.stream()
                .filter(line -> line.trim().startsWith("springboot"))
                .findFirst()
                .orElse("");
        assertThat(springbootLine)
                .as("springboot plugin must use TOML inline table syntax { id = ..., version.ref = ... }")
                .containsPattern("\\{.*id\\s*=.*version\\.ref\\s*=.*}");
    }

    @Test
    void springdependenciesPlugin_shouldUseInlineTableSyntax() {
        String springdepsLine = tomlLines.stream()
                .filter(line -> line.trim().startsWith("springdependencies"))
                .findFirst()
                .orElse("");
        assertThat(springdepsLine)
                .as("springdependencies plugin must use TOML inline table syntax")
                .containsPattern("\\{.*id\\s*=.*version\\.ref\\s*=.*}");
    }

    @Test
    void pluginEntries_shouldUseDoubleQuotesForStringValues() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);
        List<String> entryLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("["))
                .toList();
        for (String line : entryLines) {
            assertThat(line)
                    .as("TOML plugin entry must use double quotes for string values, not single quotes")
                    .doesNotContain("'");
        }
    }

    @Test
    void pluginEntries_shouldHaveBalancedBraces() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);
        List<String> entryLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("["))
                .toList();
        for (String line : entryLines) {
            long openBraces = line.chars().filter(c -> c == '{').count();
            long closeBraces = line.chars().filter(c -> c == '}').count();
            assertThat(openBraces)
                    .as("Inline table in '%s' must have balanced braces", line.trim())
                    .isEqualTo(closeBraces);
        }
    }

    @Test
    void pluginEntries_shouldNotUseSubTableSyntax() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx + "[plugins]".length());
        assertThat(pluginsSection)
                .as("Plugin entries must use inline table syntax, not sub-table syntax like [plugins.springboot]")
                .doesNotContainPattern("\\[plugins\\.");
    }

    @Test
    void tomlFile_shouldEndWithNewline() {
        assertThat(tomlContent)
                .as("TOML file must end with a newline (POSIX text file convention)")
                .endsWith("\n");
    }

    @Test
    void versionEntries_shouldUseSimpleKeyValueSyntax() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);
        List<String> versionLines = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        for (String line : versionLines) {
            assertThat(line)
                    .as("Version entry '%s' must use simple key = \"value\" syntax", line)
                    .matches("\\w+\\s*=\\s*\"[^\"]+\"");
        }
    }
}
