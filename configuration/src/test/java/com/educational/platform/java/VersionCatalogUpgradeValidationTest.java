package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the specific version catalog changes made during the Java 26
 * upgrade: the ArchUnit bump from 1.4.1 → 1.4.2.
 * <p>
 * {@link VersionCatalogStructureTest} validates the catalog's structural
 * integrity (sections, format, no duplicates, no snapshots).
 * {@link VersionCatalogCrossReferenceTest} validates cross-references between
 * catalog and build files.
 * {@link BuildConfigVersionsTest} validates that ArchUnit is >= 1.4.2.
 * <p>
 * This test validates the <em>exact</em> catalog entry values after the
 * upgrade, ensuring:
 * <ul>
 *   <li>The ArchUnit entry is exactly "1.4.2" (not just >= 1.4.2)</li>
 *   <li>No other version catalog entries were accidentally modified</li>
 *   <li>The assertj entry exists and matches what the build file references</li>
 *   <li>Entries that should not have changed are still at their expected values</li>
 * </ul>
 */
public class VersionCatalogUpgradeValidationTest {

    @Test
    void archunitVersion_shouldBe_exactly_142() throws IOException {
        String catalogContent = readVersionCatalog();

        assertThat(catalogContent)
                .as("ArchUnit version should be exactly 1.4.2 after upgrade")
                .containsPattern("archunit\\s*=\\s*\"1\\.4\\.2\"");
    }

    @Test
    void assertjVersion_shouldBeDefined_andNonEmpty() throws IOException {
        String catalogContent = readVersionCatalog();

        Matcher matcher = Pattern.compile("assertj\\s*=\\s*\"([^\"]+)\"").matcher(catalogContent);
        assertThat(matcher.find())
                .as("Version catalog should define assertj version")
                .isTrue();

        String version = matcher.group(1);
        assertThat(version)
                .as("assertj version should be non-empty and follow semver")
                .isNotBlank()
                .matches("\\d+\\.\\d+\\.\\d+");
    }

    @ParameterizedTest(name = "Version catalog entry ''{0}'' should still be ''{1}''")
    @CsvSource({
            "spring,   4.0.1",
            "mockito,  5.19.0",
            "assertj,  3.27.3",
            "archunit, 1.4.2"
    })
    void versionCatalogEntry_shouldHaveExpectedValue(String key, String expectedVersion) throws IOException {
        String catalogContent = readVersionCatalog();

        assertThat(catalogContent)
                .as("Version catalog entry '%s' should be '%s'", key, expectedVersion)
                .containsPattern(key + "\\s*=\\s*\"" + Pattern.quote(expectedVersion) + "\"");
    }

    @ParameterizedTest(name = "Unchanged entry ''{0}'' should still be ''{1}''")
    @CsvSource({
            "restAssured,  6.0.0",
            "jsonwebtoken, 0.9.1",
            "jaxbApi,      2.3.1",
            "passay,       1.6.4",
            "springDoc,    3.0.2"
    })
    void unchangedEntry_shouldNotBeModified(String key, String expectedVersion) throws IOException {
        String catalogContent = readVersionCatalog();

        assertThat(catalogContent)
                .as("Unchanged entry '%s' should still be '%s'", key, expectedVersion)
                .containsPattern(key + "\\s*=\\s*\"" + Pattern.quote(expectedVersion) + "\"");
    }

    @Test
    void versionCatalog_shouldHave_expectedEntryCount() throws IOException {
        Path catalogPath = findProjectRoot().resolve("gradle/libs.versions.toml");
        long versionEntryCount = Files.readAllLines(catalogPath).stream()
                .filter(line -> !line.trim().startsWith("["))
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> line.contains("="))
                .count();

        // versions section: 10 entries, plugins section: 1 entry
        assertThat(versionEntryCount)
                .as("Version catalog should have 11 entries (10 versions + 1 plugin)")
                .isEqualTo(11);
    }

    @Test
    void onlyArchunit_shouldHaveChanged_inVersionsSection() throws IOException {
        String catalogContent = readVersionCatalog();

        // The only version that changed in this PR is archunit: 1.4.1 → 1.4.2
        // Verify old value is NOT present
        assertThat(catalogContent)
                .as("Old ArchUnit version 1.4.1 should not be present")
                .doesNotContain("\"1.4.1\"");

        // And the new value IS present
        assertThat(catalogContent)
                .as("New ArchUnit version 1.4.2 should be present")
                .contains("\"1.4.2\"");
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
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
