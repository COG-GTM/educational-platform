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
 * Validates structural constraints of the version catalog file that are not
 * covered by existing section-level or entry-level tests. Specifically:
 * <ul>
 *   <li>The {@code [plugins]} section is the last section in the TOML file —
 *       adding unexpected sections (e.g., {@code [metadata]}, custom tables)
 *       after {@code [plugins]} could confuse Gradle's version catalog parser</li>
 *   <li>The file ends with a newline (POSIX convention)</li>
 *   <li>No trailing whitespace on any line</li>
 *   <li>No blank lines inside the {@code [plugins]} section entries</li>
 * </ul>
 * <p>
 * Complements {@link VersionCatalogSectionCompletenessTest} (section presence),
 * {@link VersionCatalogSectionOrderTest} (section ordering), and
 * {@link VersionCatalogTomlFormattingConsistencyTest} (general formatting).
 * This test focuses on termination and trailing-content invariants.
 */
class VersionCatalogPluginSectionTerminationTest {

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
    void pluginsSection_shouldBeLastSectionInFile() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx)
                .as("[plugins] section must exist")
                .isGreaterThanOrEqualTo(0);

        String afterPlugins = tomlContent.substring(pluginsIdx + "[plugins]".length());
        Pattern sectionHeader = Pattern.compile("^\\[\\w+]", Pattern.MULTILINE);
        Matcher matcher = sectionHeader.matcher(afterPlugins);
        assertThat(matcher.find())
                .as("[plugins] must be the last section in libs.versions.toml — "
                        + "no other section headers should follow; additional sections "
                        + "could confuse the Gradle version catalog parser or indicate "
                        + "accidental [bundles] or [metadata] additions")
                .isFalse();
    }

    @Test
    void pluginsSection_shouldNotContainBlankLinesBetweenEntries() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(
                pluginsIdx + "[plugins]".length()).trim();
        List<String> lines = pluginsSection.lines().toList();

        boolean insideEntries = false;
        for (String line : lines) {
            if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                insideEntries = true;
            }
            if (insideEntries && line.trim().isEmpty()) {
                // A blank line after entries started means there's a gap
                // Check if there are more entries after this blank line
                int currentIdx = pluginsSection.indexOf(line);
                String remaining = pluginsSection.substring(currentIdx).trim();
                if (!remaining.isEmpty()) {
                    assertThat(remaining.lines().anyMatch(l ->
                            !l.trim().isEmpty() && !l.trim().startsWith("#")
                                    && !l.trim().startsWith("[")))
                            .as("Plugin entries should not have blank lines between them — "
                                    + "keep all plugin entries consecutive for readability")
                            .isFalse();
                }
            }
        }
    }

    @Test
    void tomlFile_shouldEndWithNewline() {
        assertThat(tomlContent)
                .as("libs.versions.toml must end with a newline character (POSIX convention)")
                .endsWith("\n");
    }

    @Test
    void tomlFile_shouldNotHaveTrailingWhitespace() {
        List<String> linesWithTrailingWhitespace = tomlContent.lines()
                .filter(line -> !line.isEmpty() && line.endsWith(" "))
                .toList();
        assertThat(linesWithTrailingWhitespace)
                .as("No line in libs.versions.toml should have trailing whitespace")
                .isEmpty();
    }
}
