package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structural positioning and ordering of entries within
 * gradle/libs.versions.toml. TOML section ordering is not enforced by the
 * parser but is important for maintainability: [versions] must come before
 * [plugins] so that forward references (version.ref) are intuitive when
 * reading the file top-to-bottom. Additionally, the springboot plugin entry
 * must follow the existing entry (springdependencies) for alphabetical/logical
 * consistency.
 * <p>
 * Complements {@link VersionCatalogSectionCompletenessTest} (required sections
 * exist), {@link VersionCatalogSectionOrderTest} (section ordering), and
 * {@link VersionCatalogSpringbootEntryPrecisionTest} (entry content). This test
 * focuses on structural positioning that impacts developer experience.
 */
class VersionCatalogPluginDeclarationPositionTest {

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
                .as("[versions] section must exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(pluginsIdx)
                .as("[plugins] section must exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(versionsIdx)
                .as("[versions] must appear before [plugins] — "
                        + "version.ref in plugin entries references [versions] keys; "
                        + "placing [versions] first makes forward references readable")
                .isLessThan(pluginsIdx);
    }

    @Test
    void springbootEntry_shouldAppearAfterSpringdependenciesEntry() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        List<String> pluginEntries = pluginsSection.lines()
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("["))
                .filter(line -> !line.trim().startsWith("#"))
                .toList();
        int springDepsIdx = -1;
        int springBootIdx = -1;
        for (int i = 0; i < pluginEntries.size(); i++) {
            if (pluginEntries.get(i).trim().startsWith("springdependencies")) {
                springDepsIdx = i;
            }
            if (pluginEntries.get(i).trim().startsWith("springboot")) {
                springBootIdx = i;
            }
        }
        assertThat(springDepsIdx)
                .as("springdependencies entry must exist in [plugins]")
                .isGreaterThanOrEqualTo(0);
        assertThat(springBootIdx)
                .as("springboot entry must exist in [plugins]")
                .isGreaterThanOrEqualTo(0);
        assertThat(springBootIdx)
                .as("springboot entry should appear after springdependencies — "
                        + "'springboot' comes after 'springdependencies' alphabetically")
                .isGreaterThan(springDepsIdx);
    }

    @Test
    void tomlFile_shouldStartWithVersionsSection() {
        String firstNonEmptyLine = tomlLines.stream()
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("#"))
                .findFirst()
                .orElse("");
        assertThat(firstNonEmptyLine.trim())
                .as("libs.versions.toml must start with [versions] section — "
                        + "this is the conventional ordering for Gradle version catalogs")
                .isEqualTo("[versions]");
    }

    @Test
    void springVersionKey_shouldAppearFirstInVersionsSection() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        String versionsSection = tomlContent.substring(versionsIdx);
        String firstVersionEntry = versionsSection.lines()
                .skip(1) // skip the [versions] header
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("#"))
                .filter(line -> !line.trim().startsWith("["))
                .findFirst()
                .orElse("");
        assertThat(firstVersionEntry.trim())
                .as("'spring' version key should be the first entry in [versions] — "
                        + "it is the primary version driver for the project's core framework")
                .startsWith("spring");
    }

    @Test
    void pluginEntries_shouldNotHaveTrailingWhitespace() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        List<String> entriesWithTrailingSpace = pluginsSection.lines()
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("["))
                .filter(line -> line.endsWith(" ") || line.endsWith("\t"))
                .toList();
        assertThat(entriesWithTrailingSpace)
                .as("Plugin entries must not have trailing whitespace — "
                        + "trailing whitespace causes unnecessary diffs in version control")
                .isEmpty();
    }

    @Test
    void versionEntries_shouldNotHaveTrailingWhitespace() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(versionsIdx, pluginsIdx);
        List<String> entriesWithTrailingSpace = versionsSection.lines()
                .filter(line -> !line.trim().isEmpty())
                .filter(line -> !line.trim().startsWith("["))
                .filter(line -> line.endsWith(" ") || line.endsWith("\t"))
                .toList();
        assertThat(entriesWithTrailingSpace)
                .as("Version entries must not have trailing whitespace — "
                        + "trailing whitespace causes unnecessary diffs in version control")
                .isEmpty();
    }
}
