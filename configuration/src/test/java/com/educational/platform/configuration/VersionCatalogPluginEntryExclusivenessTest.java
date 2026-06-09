package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@code springboot} identifier exists exclusively in the
 * {@code [plugins]} section of {@code gradle/libs.versions.toml} and is not
 * accidentally duplicated in the {@code [libraries]} or {@code [bundles]}
 * sections. A {@code springboot} entry in {@code [libraries]} would create
 * ambiguity: {@code libs.springboot} would resolve to the library instead of
 * the plugin, causing a Gradle build-script compilation error when
 * {@code alias(libs.plugins.springboot)} tries to apply a non-existent
 * plugin accessor.
 * <p>
 * Complements {@link VersionCatalogPluginAlignmentTest} (plugin section
 * content) and {@link VersionCatalogDuplicateKeyGuardTest} (duplicate key
 * detection within sections).
 */
class VersionCatalogPluginEntryExclusivenessTest {

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
    void springboot_shouldNotAppearInLibrariesSection() {
        int librariesIdx = tomlContent.indexOf("[libraries]");
        if (librariesIdx >= 0) {
            int nextSectionIdx = tomlContent.indexOf("[", librariesIdx + 1);
            String librariesSection = nextSectionIdx >= 0
                    ? tomlContent.substring(librariesIdx, nextSectionIdx)
                    : tomlContent.substring(librariesIdx);
            boolean hasSpringboot = librariesSection.lines()
                    .filter(line -> !line.trim().startsWith("#"))
                    .filter(line -> !line.trim().startsWith("["))
                    .anyMatch(line -> line.trim().startsWith("springboot"));
            assertThat(hasSpringboot)
                    .as("'springboot' must NOT appear in [libraries] section — "
                            + "it is a Gradle plugin, not a library dependency; "
                            + "having it in both sections causes accessor ambiguity")
                    .isFalse();
        }
    }

    @Test
    void springboot_shouldNotAppearInBundlesSection() {
        int bundlesIdx = tomlContent.indexOf("[bundles]");
        if (bundlesIdx >= 0) {
            int nextSectionIdx = tomlContent.indexOf("[", bundlesIdx + 1);
            String bundlesSection = nextSectionIdx >= 0
                    ? tomlContent.substring(bundlesIdx, nextSectionIdx)
                    : tomlContent.substring(bundlesIdx);
            boolean hasSpringboot = bundlesSection.lines()
                    .filter(line -> !line.trim().startsWith("#"))
                    .filter(line -> !line.trim().startsWith("["))
                    .anyMatch(line -> line.trim().startsWith("springboot"));
            assertThat(hasSpringboot)
                    .as("'springboot' must NOT appear in [bundles] section — "
                            + "plugins are not bundled; they are applied individually")
                    .isFalse();
        }
    }

    @Test
    void springboot_shouldNotAppearInVersionsSection() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int nextSectionIdx = tomlContent.indexOf("[", versionsIdx + 1);
        String versionsSection = nextSectionIdx >= 0
                ? tomlContent.substring(versionsIdx, nextSectionIdx)
                : tomlContent.substring(versionsIdx);
        boolean hasSpringbootKey = versionsSection.lines()
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> !line.trim().startsWith("["))
                .anyMatch(line -> line.trim().startsWith("springboot"));
        assertThat(hasSpringbootKey)
                .as("'springboot' must NOT be a version key — "
                        + "the plugin uses version.ref = \"spring\" to share the "
                        + "Spring Framework version with the BOM; a separate "
                        + "'springboot' version key would create drift risk")
                .isFalse();
    }

    @Test
    void springboot_shouldAppearExactlyOnceInEntireFile() {
        long count = tomlContent.lines()
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> line.trim().startsWith("springboot"))
                .count();
        assertThat(count)
                .as("'springboot' entry must appear exactly once in the entire TOML file "
                        + "(in the [plugins] section only)")
                .isEqualTo(1);
    }
}
