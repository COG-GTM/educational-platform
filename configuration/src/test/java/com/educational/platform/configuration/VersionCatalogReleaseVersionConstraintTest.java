package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that all version values declared in gradle/libs.versions.toml
 * are GA (General Availability) release versions, not pre-release qualifiers.
 * The PR added the springboot plugin referencing the existing "spring" version
 * (4.0.1). Using SNAPSHOT, RC, M (Milestone), or BETA qualifiers in the
 * version catalog would make builds non-reproducible and potentially unstable.
 */
class VersionCatalogReleaseVersionConstraintTest {

    private static List<String> versionValues;

    @BeforeAll
    static void loadVersionValues() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        String tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));

        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);

        Pattern versionValuePattern = Pattern.compile("=\\s*\"([^\"]+)\"");
        versionValues = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> {
                    Matcher m = versionValuePattern.matcher(line);
                    return m.find() ? m.group(1) : "";
                })
                .filter(v -> !v.isEmpty())
                .toList();
    }

    @Test
    void allVersions_shouldNotContainSnapshotQualifier() {
        for (String version : versionValues) {
            assertThat(version.toUpperCase())
                    .as("Version '%s' must not be a SNAPSHOT — snapshots are non-reproducible "
                            + "and should not be committed to the version catalog", version)
                    .doesNotContain("SNAPSHOT");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-RC", "-M", "-BETA", "-ALPHA", ".RC", ".M", ".BETA", ".ALPHA"})
    void allVersions_shouldNotContainPreReleaseQualifier(String qualifier) {
        for (String version : versionValues) {
            assertThat(version.toUpperCase())
                    .as("Version '%s' must not contain pre-release qualifier '%s' — "
                            + "only GA releases should be used in the version catalog",
                            version, qualifier)
                    .doesNotContain(qualifier.toUpperCase());
        }
    }

    @Test
    void allVersions_shouldStartWithNumericMajorVersion() {
        for (String version : versionValues) {
            assertThat(version)
                    .as("Version '%s' must start with a numeric major version (semantic versioning)", version)
                    .matches("^\\d+\\..*");
        }
    }

    @Test
    void allVersions_shouldBeNonEmpty() {
        assertThat(versionValues)
                .as("At least one version must be declared in [versions]")
                .isNotEmpty();
        for (String version : versionValues) {
            assertThat(version)
                    .as("Version values must not be blank")
                    .isNotBlank();
        }
    }

    @Test
    void springVersion_shouldBeGaRelease() {
        assertThat(versionValues)
                .as("Version list must contain the spring version")
                .anyMatch(v -> v.startsWith("4.0"));
        String springVersion = versionValues.stream()
                .filter(v -> v.startsWith("4.0"))
                .findFirst().orElse("");
        assertThat(springVersion)
                .as("Spring version must be a GA release matching semver (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+");
    }
}
