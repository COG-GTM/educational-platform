package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the formatting consistency of gradle/libs.versions.toml after
 * the PR added the springboot plugin entry. Inconsistent formatting (mixed
 * quoting styles, inconsistent spacing, trailing whitespace) creates noisy
 * diffs and makes it harder to review version catalog changes. These tests
 * enforce a minimal formatting contract that ensures the file remains clean
 * and parseable by Gradle's TOML parser.
 * <p>
 * Complements {@link VersionCatalogStructuralIntegrityTest} (section presence,
 * key validity) and {@link VersionCatalogSectionCompletenessTest} (entry counts).
 */
class VersionCatalogTomlFormattingConsistencyTest {

    private static List<String> tomlLines;
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
        tomlLines = tomlContent.lines().toList();
    }

    @Test
    void tomlLines_shouldNotHaveTrailingWhitespace() {
        for (int i = 0; i < tomlLines.size(); i++) {
            String line = tomlLines.get(i);
            if (!line.isEmpty()) {
                assertThat(line)
                        .as("Line %d must not have trailing whitespace — "
                                + "trailing spaces cause noisy diffs", i + 1)
                        .doesNotEndWith(" ")
                        .doesNotEndWith("\t");
            }
        }
    }

    @Test
    void tomlValueLines_shouldUseDoubleQuotesConsistently() {
        List<String> valueLines = tomlLines.stream()
                .filter(line -> line.contains("="))
                .filter(line -> !line.trim().startsWith("#"))
                .toList();
        for (String line : valueLines) {
            assertThat(line)
                    .as("TOML value lines must use double quotes (not single quotes) — "
                            + "TOML spec requires double quotes for string values: %s", line)
                    .doesNotContain("'");
        }
    }

    @Test
    void tomlKeyValueLines_shouldHaveSpacesAroundEquals() {
        List<String> kvLines = tomlLines.stream()
                .filter(line -> line.contains("=") && !line.trim().startsWith("["))
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> !line.trim().isEmpty())
                .toList();
        for (String line : kvLines) {
            String trimmed = line.trim();
            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                assertThat(trimmed.charAt(eqIdx - 1))
                        .as("Key-value line must have a space before '=': %s", trimmed)
                        .isEqualTo(' ');
                assertThat(trimmed.charAt(eqIdx + 1))
                        .as("Key-value line must have a space after '=': %s", trimmed)
                        .isEqualTo(' ');
            }
        }
    }

    @Test
    void tomlFile_shouldEndWithNewline() {
        assertThat(tomlContent)
                .as("TOML file must end with a newline character (POSIX convention)")
                .endsWith("\n");
    }

    @Test
    void tomlFile_shouldNotContainWindowsLineEndings() {
        assertThat(tomlContent)
                .as("TOML file must use Unix line endings (LF), not Windows (CRLF)")
                .doesNotContain("\r\n");
    }

    @Test
    void tomlFile_shouldNotContainTabIndentation() {
        for (int i = 0; i < tomlLines.size(); i++) {
            assertThat(tomlLines.get(i))
                    .as("Line %d must not use tab indentation — "
                            + "TOML keys are not indented in this project", i + 1)
                    .doesNotContain("\t");
        }
    }

    @Test
    void tomlSectionHeaders_shouldHaveNoLeadingWhitespace() {
        List<String> sectionLines = tomlLines.stream()
                .filter(line -> line.contains("[") && line.contains("]"))
                .toList();
        for (String line : sectionLines) {
            assertThat(line)
                    .as("TOML section headers must start at column 0: %s", line)
                    .matches("^\\[.*]$");
        }
    }
}
