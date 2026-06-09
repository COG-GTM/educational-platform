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
 * Validates the structural ordering and section layout of gradle/libs.versions.toml.
 * Gradle's version catalog parser requires sections to appear in a specific order:
 * {@code [versions]} must precede {@code [plugins]} so that version.ref entries
 * can resolve forward references. Additionally, no unexpected sections
 * (e.g., {@code [libraries]}, {@code [bundles]}) should appear unless the project
 * architecture explicitly requires them.
 * <p>
 * Complements {@link VersionCatalogStructuralIntegrityTest} (overall TOML validity)
 * and {@link VersionCatalogSectionCompletenessTest} (presence of required sections).
 */
class VersionCatalogSectionOrderTest {

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
    void versionsSection_shouldAppearBeforePluginsSection() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(versionsIdx)
                .as("[versions] section must be declared — "
                        + "it defines version keys referenced by plugin entries")
                .isGreaterThanOrEqualTo(0);
        assertThat(pluginsIdx)
                .as("[plugins] section must be declared — "
                        + "it defines the springboot and springdependencies plugins")
                .isGreaterThanOrEqualTo(0);
        assertThat(versionsIdx)
                .as("[versions] must appear before [plugins] so that version.ref "
                        + "entries in [plugins] can resolve against [versions] keys")
                .isLessThan(pluginsIdx);
    }

    @Test
    void versionsSection_shouldBeTheFirstSection() {
        String firstSectionHeader = tomlLines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("[") && line.endsWith("]"))
                .findFirst()
                .orElse("");
        assertThat(firstSectionHeader)
                .as("The first section in libs.versions.toml must be [versions]")
                .isEqualTo("[versions]");
    }

    @Test
    void tomlFile_shouldContainExactlyTwoSections() {
        long sectionCount = tomlLines.stream()
                .map(String::trim)
                .filter(line -> Pattern.matches("^\\[[a-zA-Z]+]$", line))
                .count();
        assertThat(sectionCount)
                .as("libs.versions.toml must contain exactly 2 sections: [versions] and [plugins] — "
                        + "adding [libraries] or [bundles] without architectural justification "
                        + "increases complexity and may conflict with BOM-managed dependencies")
                .isEqualTo(2);
    }

    @Test
    void tomlFile_shouldNotContainLibrariesSection() {
        assertThat(tomlContent)
                .as("[libraries] section must NOT be present — "
                        + "dependency declarations use inline group/artifact notation in build files, "
                        + "with versions managed by the Spring Boot BOM")
                .doesNotContain("[libraries]");
    }

    @Test
    void tomlFile_shouldNotContainBundlesSection() {
        assertThat(tomlContent)
                .as("[bundles] section must NOT be present — "
                        + "dependency grouping is handled by Spring Boot starters, not TOML bundles")
                .doesNotContain("[bundles]");
    }

    @Test
    void noContentShouldAppearBeforeFirstSection() {
        List<String> beforeFirstSection = tomlLines.stream()
                .takeWhile(line -> !line.trim().startsWith("["))
                .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                .toList();
        assertThat(beforeFirstSection)
                .as("No key-value entries should appear before the first section header — "
                        + "TOML requires entries to be within a section")
                .isEmpty();
    }
}
