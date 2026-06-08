package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that every {@code version.ref} value declared in the [plugins]
 * section of gradle/libs.versions.toml resolves to an existing key in the
 * [versions] section. A typo in version.ref (e.g., "sprng" instead of "spring")
 * would cause Gradle to fail with "version catalog key not found" at
 * configuration time, breaking all builds including bootRun and bootJar.
 *
 * Complements {@link VersionCatalogPluginCountInvariantTest} (which guards
 * the exact plugin set) and {@link GradleBuildFileValidationTest} (which
 * checks the plugin is referenced correctly in the build file).
 */
class VersionCatalogVersionRefResolutionTest {

    private static String tomlContent;
    private static Set<String> declaredVersionKeys;
    private static List<String> pluginEntryLines;

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

        // Extract declared version keys from [versions] section
        int versionsIdx = tomlContent.indexOf("[versions]");
        assertThat(versionsIdx).isGreaterThanOrEqualTo(0);
        String afterVersions = tomlContent.substring(versionsIdx + "[versions]".length());
        int nextSectionIdx = afterVersions.indexOf("[");
        String versionsSection = nextSectionIdx >= 0
                ? afterVersions.substring(0, nextSectionIdx)
                : afterVersions;
        declaredVersionKeys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                .map(line -> line.split("\\s*=")[0].trim())
                .collect(Collectors.toSet());

        // Extract plugin entry lines from [plugins] section
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx + "[plugins]".length());
        pluginEntryLines = pluginsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("["))
                .toList();
    }

    @Test
    void allPluginVersionRefs_shouldResolveToExistingVersionKeys() {
        Pattern versionRefPattern = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"");
        for (String line : pluginEntryLines) {
            Matcher m = versionRefPattern.matcher(line);
            assertThat(m.find())
                    .as("Plugin entry must declare a version.ref: %s", line)
                    .isTrue();
            String ref = m.group(1);
            assertThat(declaredVersionKeys)
                    .as("version.ref '%s' in plugin entry '%s' must resolve to "
                            + "an existing key in [versions] section — a typo here "
                            + "breaks Gradle configuration for all modules", ref, line)
                    .contains(ref);
        }
    }

    @Test
    void springbootPlugin_shouldReferenceSpringVersionKey() {
        String springbootLine = pluginEntryLines.stream()
                .filter(line -> line.startsWith("springboot"))
                .findFirst()
                .orElse("");
        assertThat(springbootLine)
                .as("springboot plugin entry must exist")
                .isNotEmpty();
        assertThat(springbootLine)
                .as("springboot plugin must reference version.ref = \"spring\" "
                        + "to align with the BOM import in the root build")
                .containsPattern("version\\.ref\\s*=\\s*\"spring\"");
    }

    @Test
    void springdependenciesPlugin_shouldNotReferenceSpringVersionKey() {
        String springDepsLine = pluginEntryLines.stream()
                .filter(line -> line.startsWith("springdependencies"))
                .findFirst()
                .orElse("");
        assertThat(springDepsLine)
                .as("springdependencies plugin entry must exist")
                .isNotEmpty();
        // springdependencies has its own version (1.1.7), not the Spring Boot version
        assertThat(springDepsLine)
                .as("springdependencies plugin must NOT use version.ref = \"spring\" — "
                        + "it is a separate plugin (io.spring.dependency-management) "
                        + "with its own release cadence")
                .doesNotContainPattern("version\\.ref\\s*=\\s*\"spring\"");
    }

    @Test
    void versionKeys_shouldNotContainDuplicates() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        String afterVersions = tomlContent.substring(versionsIdx + "[versions]".length());
        int nextSectionIdx = afterVersions.indexOf("[");
        String versionsSection = nextSectionIdx >= 0
                ? afterVersions.substring(0, nextSectionIdx)
                : afterVersions;
        List<String> keys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
        assertThat(keys)
                .as("Version keys must be unique — duplicates cause TOML parse errors")
                .doesNotHaveDuplicates();
    }
}
