package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the version catalog (gradle/libs.versions.toml) has no
 * duplicate keys within any section. TOML silently overwrites earlier
 * entries when a duplicate key exists, which would cause the Spring Boot
 * plugin or BOM version to resolve to an unintended value. The PR added
 * a new {@code springboot} entry to [plugins] and reuses the existing
 * {@code spring} key in [versions]; this test guards against accidental
 * duplication introduced by future edits.
 * <p>
 * Complements {@link VersionCatalogPluginIdUniquenessTest} which validates
 * plugin ID uniqueness, and {@link VersionCatalogStructuralIntegrityTest}
 * which validates overall TOML structure.
 */
class VersionCatalogDuplicateKeyGuardTest {

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
    void versionsSection_shouldHaveNoDuplicateKeys() {
        List<String> keys = extractKeysFromSection("[versions]");
        assertThat(keys)
                .as("[versions] section must have no duplicate keys — "
                        + "TOML silently overwrites earlier entries on duplicate, "
                        + "which could cause the Spring Boot BOM to resolve the wrong version")
                .doesNotHaveDuplicates();
    }

    @Test
    void pluginsSection_shouldHaveNoDuplicateKeys() {
        List<String> keys = extractKeysFromSection("[plugins]");
        assertThat(keys)
                .as("[plugins] section must have no duplicate keys — "
                        + "a duplicate springboot alias would silently overwrite the first entry")
                .doesNotHaveDuplicates();
    }

    @Test
    void noKeyAppearsInBothSections() {
        List<String> versionKeys = extractKeysFromSection("[versions]");
        List<String> pluginKeys = extractKeysFromSection("[plugins]");
        for (String pluginKey : pluginKeys) {
            assertThat(versionKeys)
                    .as("Plugin alias '%s' must not collide with a version key — "
                            + "Gradle uses the key to generate type-safe accessors and "
                            + "collisions would cause compilation errors in build scripts",
                            pluginKey)
                    .doesNotContain(pluginKey);
        }
    }

    @Test
    void allKeysAcrossSections_shouldBeNonBlank() {
        List<String> allKeys = new ArrayList<>();
        allKeys.addAll(extractKeysFromSection("[versions]"));
        allKeys.addAll(extractKeysFromSection("[plugins]"));
        for (String key : allKeys) {
            assertThat(key)
                    .as("Version catalog keys must be non-blank")
                    .isNotBlank();
        }
    }

    private List<String> extractKeysFromSection(String sectionHeader) {
        int sectionIdx = tomlContent.indexOf(sectionHeader);
        if (sectionIdx < 0) return List.of();
        String afterHeader = tomlContent.substring(sectionIdx + sectionHeader.length());
        int nextSectionIdx = afterHeader.indexOf("[");
        String section = nextSectionIdx >= 0
                ? afterHeader.substring(0, nextSectionIdx)
                : afterHeader;
        return section.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
    }
}
