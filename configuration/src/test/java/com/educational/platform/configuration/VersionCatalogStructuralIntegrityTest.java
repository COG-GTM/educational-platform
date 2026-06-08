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
 * Validates the structural integrity of gradle/libs.versions.toml
 * to guard against misconfigurations that would cause Gradle resolution failures.
 * The PR added a new plugin entry (springboot) with a version.ref; these tests
 * ensure the TOML file remains well-formed and internally consistent.
 */
class VersionCatalogStructuralIntegrityTest {

    private static Path projectRoot;
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
        projectRoot = dir;
        tomlContent = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void toml_shouldContainVersionsSection() {
        assertThat(tomlContent)
                .as("libs.versions.toml must have a [versions] section")
                .contains("[versions]");
    }

    @Test
    void toml_shouldContainPluginsSection() {
        assertThat(tomlContent)
                .as("libs.versions.toml must have a [plugins] section")
                .contains("[plugins]");
    }

    @Test
    void toml_versionsSectionShouldAppearBeforePluginsSection() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(versionsIdx)
                .as("[versions] section must appear before [plugins] section in TOML")
                .isLessThan(pluginsIdx);
    }

    @Test
    void toml_allPluginVersionRefs_shouldReferenceExistingVersions() {
        // Extract all version.ref = "xxx" from the [plugins] section
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);

        Pattern versionRefPattern = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"");
        Matcher matcher = versionRefPattern.matcher(pluginsSection);

        while (matcher.find()) {
            String referencedVersion = matcher.group(1);
            // Verify this version key exists in the [versions] section
            Pattern versionKeyPattern = Pattern.compile(
                    "^" + Pattern.quote(referencedVersion) + "\\s*=",
                    Pattern.MULTILINE
            );
            assertThat(versionKeyPattern.matcher(tomlContent).find())
                    .as("Plugin version.ref '%s' must reference an existing entry in [versions] section",
                            referencedVersion)
                    .isTrue();
        }
    }

    @Test
    void toml_springbootPlugin_shouldNotUseInlineVersion() {
        // version.ref is preferred over inline version for BOM alignment
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        List<String> lines = pluginsSection.lines().toList();

        for (String line : lines) {
            if (line.contains("springboot")) {
                assertThat(line)
                        .as("springboot plugin must use version.ref (not inline version) for alignment with BOM")
                        .contains("version.ref")
                        .doesNotContainPattern("version\\s*=\\s*\"\\d");
                break;
            }
        }
    }

    @Test
    void toml_springVersion_shouldBeValidSemanticVersion() {
        Pattern springVersionPattern = Pattern.compile(
                "^spring\\s*=\\s*\"(\\d+\\.\\d+\\.\\d+.*)\"",
                Pattern.MULTILINE
        );
        Matcher matcher = springVersionPattern.matcher(tomlContent);
        assertThat(matcher.find())
                .as("spring version must be a valid semantic version (major.minor.patch)")
                .isTrue();
    }

    @Test
    void toml_shouldNotHaveDuplicatePluginEntries() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        long springbootCount = pluginsSection.lines()
                .filter(line -> line.trim().startsWith("springboot"))
                .count();
        assertThat(springbootCount)
                .as("There must be exactly one springboot plugin entry in [plugins]")
                .isEqualTo(1);
    }

    @Test
    void toml_pluginIds_shouldBeValidGradlePluginCoordinates() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        Pattern pluginIdPattern = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pluginIdPattern.matcher(pluginsSection);

        while (matcher.find()) {
            String pluginId = matcher.group(1);
            assertThat(pluginId)
                    .as("Plugin ID '%s' must be a valid dot-separated Gradle plugin coordinate", pluginId)
                    .matches("[a-z][a-z0-9]*(\\.[a-z][a-z0-9-]*)+");
        }
    }
}
