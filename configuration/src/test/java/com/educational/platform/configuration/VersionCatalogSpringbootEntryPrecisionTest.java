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
 * Validates the precise format and content of the {@code springboot} entry
 * in the {@code [plugins]} section of {@code gradle/libs.versions.toml}.
 * Existing tests validate that the entry exists
 * ({@link VersionCatalogPluginAlignmentTest}), that it uses {@code version.ref}
 * ({@link VersionCatalogPluginVersionRefAlignmentTest}), and that the plugin
 * id is correct ({@link VersionCatalogPluginIdValidationTest}). This test
 * validates the exact inline-table format: single-line declaration with both
 * {@code id} and {@code version.ref} fields, no extraneous attributes like
 * {@code apply}, and proper TOML quoting. A malformed entry would cause
 * Gradle to fail during settings evaluation before any build logic runs.
 */
class VersionCatalogSpringbootEntryPrecisionTest {

    private static String tomlContent;
    private static String pluginsSection;

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
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        pluginsSection = tomlContent.substring(pluginsIdx);
    }

    @Test
    void springbootEntry_shouldBeASingleLineInlineTable() {
        List<String> springbootLines = pluginsSection.lines()
                .filter(line -> line.trim().startsWith("springboot"))
                .toList();
        assertThat(springbootLines)
                .as("springboot entry must appear exactly once in [plugins] section")
                .hasSize(1);
        String line = springbootLines.getFirst().trim();
        assertThat(line)
                .as("springboot entry must be a single-line TOML inline table — "
                        + "multi-line table syntax would work but breaks the project's "
                        + "convention of one-line plugin entries")
                .contains("{")
                .contains("}");
    }

    @Test
    void springbootEntry_shouldContainBothIdAndVersionRef() {
        String line = pluginsSection.lines()
                .filter(l -> l.trim().startsWith("springboot"))
                .findFirst().orElse("");
        assertThat(line)
                .as("springboot entry must declare both 'id' and 'version.ref' attributes")
                .containsPattern("id\\s*=\\s*\"[^\"]+\"")
                .containsPattern("version\\.ref\\s*=\\s*\"[^\"]+\"");
    }

    @Test
    void springbootEntry_shouldNotContainApplyAttribute() {
        String line = pluginsSection.lines()
                .filter(l -> l.trim().startsWith("springboot"))
                .findFirst().orElse("");
        assertThat(line)
                .as("springboot TOML entry must NOT contain an 'apply' attribute — "
                        + "the version catalog should not control plugin application; "
                        + "that is the build script's responsibility")
                .doesNotContainPattern("apply\\s*=");
    }

    @Test
    void springbootEntry_versionRef_shouldReferenceSpringKey() {
        String line = pluginsSection.lines()
                .filter(l -> l.trim().startsWith("springboot"))
                .findFirst().orElse("");
        Matcher m = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"").matcher(line);
        assertThat(m.find())
                .as("springboot entry must have a parseable version.ref attribute")
                .isTrue();
        assertThat(m.group(1))
                .as("springboot version.ref must point to 'spring' key — "
                        + "this ensures the plugin version is aligned with the BOM version "
                        + "managed by io.spring.dependency-management")
                .isEqualTo("spring");
    }

    @Test
    void springbootEntry_shouldUseDoubleQuotedStrings() {
        String line = pluginsSection.lines()
                .filter(l -> l.trim().startsWith("springboot"))
                .findFirst().orElse("");
        assertThat(line)
                .as("TOML entries must use double-quoted strings, not single quotes")
                .doesNotContain("'");
    }

    @Test
    void springbootEntry_shouldBeLastInPluginsSection() {
        List<String> pluginEntries = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("[") && !line.startsWith("#"))
                .toList();
        assertThat(pluginEntries).isNotEmpty();
        assertThat(pluginEntries.getLast())
                .as("springboot should be the last entry in [plugins] — "
                        + "maintaining alphabetical or logical ordering")
                .startsWith("springboot");
    }
}
