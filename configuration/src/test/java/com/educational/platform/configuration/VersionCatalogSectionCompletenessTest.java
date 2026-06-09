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
 * Validates the structural completeness of gradle/libs.versions.toml
 * after the PR added the springboot plugin entry. Guards against:
 * <ul>
 *   <li>Accidental addition of a [libraries] section that could declare
 *       Spring Boot artifacts with versions conflicting with the BOM</li>
 *   <li>Accidental addition of a [bundles] section that might bundle
 *       incompatible plugin combinations</li>
 *   <li>Plugin entry count drift (exactly 2 plugins: springdependencies
 *       and springboot)</li>
 *   <li>Version entry proliferation that might create confusion about
 *       which version key controls the Spring Boot plugin</li>
 * </ul>
 */
class VersionCatalogSectionCompletenessTest {

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
    void toml_shouldNotContainLibrariesSection() {
        assertThat(tomlContent)
                .as("libs.versions.toml must NOT have a [libraries] section — "
                        + "Spring Boot artifact versions are managed by the BOM imported via "
                        + "io.spring.dependency-management, not declared in the version catalog")
                .doesNotContain("[libraries]");
    }

    @Test
    void toml_shouldNotContainBundlesSection() {
        assertThat(tomlContent)
                .as("libs.versions.toml must NOT have a [bundles] section — "
                        + "plugins are applied individually and should not be bundled")
                .doesNotContain("[bundles]");
    }

    @Test
    void toml_pluginsSection_shouldContainExactlyTwoEntries() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx + "[plugins]".length());
        // Count non-empty, non-comment lines until the next section or end of file
        long entryCount = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.startsWith("["))
                .count();
        assertThat(entryCount)
                .as("The [plugins] section must contain exactly 2 entries "
                        + "(springdependencies and springboot)")
                .isEqualTo(2);
    }

    @Test
    void toml_shouldHaveExactlyTwoSections() {
        Pattern sectionPattern = Pattern.compile("^\\[\\w+]", Pattern.MULTILINE);
        List<String> sections = sectionPattern.matcher(tomlContent).results()
                .map(m -> m.group())
                .toList();
        assertThat(sections)
                .as("libs.versions.toml must have exactly two sections: [versions] and [plugins]")
                .containsExactly("[versions]", "[plugins]");
    }

    @Test
    void toml_shouldNotContainDuplicateVersionKeys() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);
        List<String> versionKeys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
        assertThat(versionKeys)
                .as("All version keys in [versions] must be unique — duplicates cause "
                        + "Gradle resolution ambiguity")
                .doesNotHaveDuplicates();
    }

    @Test
    void toml_shouldNotDeclareSpringBootVersionSeparateFromSpring() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);
        assertThat(versionsSection)
                .as("There must be no separate 'springBoot' or 'spring-boot' version key — "
                        + "the springboot plugin references version.ref = \"spring\" which is "
                        + "shared with the BOM for alignment")
                .doesNotContainPattern("(?i)spring[_-]?boot\\s*=");
    }
}
