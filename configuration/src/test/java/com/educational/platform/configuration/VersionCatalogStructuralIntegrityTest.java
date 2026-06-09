package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structural integrity of gradle/libs.versions.toml beyond
 * the plugin entries tested by {@link VersionCatalogPluginCountInvariantTest}.
 * The Spring Boot plugin's version catalog resolution depends on proper TOML
 * structure — section ordering, no duplicate headers, and correct inline-table
 * format for plugin declarations. A malformed TOML file causes Gradle to fail
 * during settings evaluation, before any task (bootRun, bootJar) can execute.
 */
class VersionCatalogStructuralIntegrityTest {

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
    void tomlFile_shouldHaveVersionsSectionBeforePluginsSection() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(versionsIdx)
                .as("[versions] section must be present")
                .isGreaterThanOrEqualTo(0);
        assertThat(pluginsIdx)
                .as("[plugins] section must be present")
                .isGreaterThanOrEqualTo(0);
        assertThat(versionsIdx)
                .as("[versions] section must appear before [plugins] section — "
                        + "Gradle resolves version.ref from [versions] so declaring it "
                        + "first follows the dependency-before-use convention")
                .isLessThan(pluginsIdx);
    }

    @Test
    void tomlFile_shouldNotHaveDuplicateSectionHeaders() {
        List<String> sectionHeaders = tomlLines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("[") && !line.startsWith("[["))
                .toList();
        assertThat(sectionHeaders)
                .as("TOML file must not have duplicate section headers — "
                        + "duplicates cause parsing errors in Gradle's version catalog loader")
                .doesNotHaveDuplicates();
    }

    @Test
    void tomlFile_shouldContainExactlyTwoSections() {
        List<String> sectionHeaders = tomlLines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("[") && !line.startsWith("[["))
                .toList();
        assertThat(sectionHeaders)
                .as("Version catalog must contain exactly [versions] and [plugins] sections — "
                        + "additional sections ([libraries], [bundles]) are not needed for this project "
                        + "and could introduce unused managed dependencies")
                .containsExactly("[versions]", "[plugins]");
    }

    @Test
    void pluginEntries_shouldUseInlineTableFormat() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx + "[plugins]".length());
        List<String> entryLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("["))
                .toList();
        Pattern inlineTablePattern = Pattern.compile(
                "^\\w+\\s*=\\s*\\{.*id\\s*=\\s*\"[^\"]+\".*version\\.ref\\s*=\\s*\"\\w+\".*}$");
        for (String line : entryLines) {
            assertThat(line)
                    .as("Plugin entry must use inline table format { id = \"...\", version.ref = \"...\" }: %s",
                            line)
                    .matches(inlineTablePattern);
        }
    }

    @Test
    void versionEntries_shouldUseSimpleKeyValueFormat() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        String afterVersions = tomlContent.substring(versionsIdx + "[versions]".length());
        int nextSectionIdx = afterVersions.indexOf("[");
        String versionsSection = nextSectionIdx >= 0
                ? afterVersions.substring(0, nextSectionIdx)
                : afterVersions;
        List<String> entryLines = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .toList();
        Pattern simpleKVPattern = Pattern.compile("^\\w+\\s*=\\s*\"[^\"]+\"$");
        for (String line : entryLines) {
            assertThat(line)
                    .as("Version entry must use simple key = \"value\" format: %s", line)
                    .matches(simpleKVPattern);
        }
    }

    @Test
    void tomlFile_shouldNotContainBlankLinesInsideSections() {
        // Blank lines between entries within a section are allowed by TOML
        // but not conventional in Gradle version catalogs; guard against
        // accidental double-blank-lines that may indicate merge conflicts
        assertThat(tomlContent)
                .as("TOML file should not contain triple newlines (indicator of merge conflict residue)")
                .doesNotContain("\n\n\n");
    }

    @Test
    void tomlFile_shouldNotContainWindowsLineEndings() {
        assertThat(tomlContent)
                .as("TOML file must use Unix line endings (LF) — "
                        + "Windows line endings (CRLF) can cause issues with some TOML parsers")
                .doesNotContain("\r\n");
    }
}
