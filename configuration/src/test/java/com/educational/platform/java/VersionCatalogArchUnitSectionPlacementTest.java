package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the placement of the ArchUnit version entry within the
 * Gradle version catalog's {@code [versions]} section.
 * <p>
 * The Java 26 upgrade bumped {@code archunit} from {@code "1.4.1"} to
 * {@code "1.4.2"} in {@code gradle/libs.versions.toml}. The TOML file
 * has multiple sections ({@code [versions]}, {@code [plugins]}). If the
 * entry is accidentally placed in the wrong section, Gradle resolves it
 * as a plugin reference instead of a library version, causing build failures.
 * <p>
 * {@link VersionCatalogStructureTest} validates overall TOML structure.
 * {@link VersionCatalogCrossReferenceTest} validates cross-references.
 * This test validates <em>section placement</em> of upgraded entries.
 */
public class VersionCatalogArchUnitSectionPlacementTest {

    @Test
    void archunitEntry_shouldAppear_inVersionsSection() throws IOException {
        List<String> lines = readVersionCatalogLines();

        String currentSection = "";
        boolean archunitFoundInVersions = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed;
            }
            if (trimmed.startsWith("archunit") && trimmed.contains("=")) {
                if ("[versions]".equals(currentSection)) {
                    archunitFoundInVersions = true;
                }
            }
        }

        assertThat(archunitFoundInVersions)
                .as("archunit version entry should be in the [versions] section")
                .isTrue();
    }

    @Test
    void archunitEntry_shouldNotAppear_inPluginsSection() throws IOException {
        List<String> lines = readVersionCatalogLines();

        String currentSection = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed;
            }
            if (trimmed.startsWith("archunit") && trimmed.contains("=")) {
                assertThat(currentSection)
                        .as("archunit should not appear in %s", currentSection)
                        .isNotEqualTo("[plugins]");
            }
        }
    }

    @ParameterizedTest(name = "Catalog key ''{0}'' should be in [versions] section")
    @ValueSource(strings = {"archunit", "mockito", "assertj"})
    void testDependencyVersion_shouldBe_inVersionsSection(String key) throws IOException {
        List<String> lines = readVersionCatalogLines();

        String currentSection = "";
        boolean foundInVersions = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed;
            }
            if (trimmed.startsWith(key) && trimmed.contains("=") && "[versions]".equals(currentSection)) {
                foundInVersions = true;
            }
        }

        assertThat(foundInVersions)
                .as("'%s' should be defined in the [versions] section", key)
                .isTrue();
    }

    @Test
    void versionsSection_shouldAppear_beforePluginsSection() throws IOException {
        List<String> lines = readVersionCatalogLines();

        int versionsLine = -1;
        int pluginsLine = -1;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if ("[versions]".equals(trimmed) && versionsLine == -1) {
                versionsLine = i;
            }
            if ("[plugins]".equals(trimmed) && pluginsLine == -1) {
                pluginsLine = i;
            }
        }

        assertThat(versionsLine)
                .as("[versions] section should exist")
                .isGreaterThanOrEqualTo(0);

        if (pluginsLine >= 0) {
            assertThat(versionsLine)
                    .as("[versions] should appear before [plugins] by convention")
                    .isLessThan(pluginsLine);
        }
    }

    @Test
    void archunitVersionValue_shouldBe_semverInVersionsSection() throws IOException {
        String content = readVersionCatalog();
        Matcher m = Pattern.compile("archunit\\s*=\\s*\"(\\d+\\.\\d+\\.\\d+)\"").matcher(content);

        assertThat(m.find())
                .as("archunit should have a semver value in the catalog")
                .isTrue();

        String version = m.group(1);
        assertThat(version)
                .as("archunit version should be exactly 1.4.2")
                .isEqualTo("1.4.2");
    }

    @Test
    void archunitEntry_shouldNotBeDuplicated_acrossSections() throws IOException {
        List<String> lines = readVersionCatalogLines();

        long archunitEntryCount = lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("archunit") && line.contains("="))
                .count();

        assertThat(archunitEntryCount)
                .as("archunit should appear exactly once in the catalog")
                .isEqualTo(1);
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
    }

    private List<String> readVersionCatalogLines() throws IOException {
        return Files.readAllLines(findProjectRoot().resolve("gradle/libs.versions.toml"));
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path settingsFile = current.resolve("settings.gradle.kts");
            Path gradleDir = current.resolve("gradle/wrapper");
            if (Files.exists(settingsFile) || Files.isDirectory(gradleDir)) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
