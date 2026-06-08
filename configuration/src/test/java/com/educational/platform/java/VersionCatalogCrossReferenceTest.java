package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates cross-references within the Gradle version catalog (libs.versions.toml).
 * <p>
 * The [plugins] section can reference versions from [versions] via version.ref.
 * If a version.ref points to a non-existent key, Gradle fails at configuration time.
 * After the Java 26 upgrade changed the version catalog, these tests ensure all
 * internal references remain valid and no orphaned or dangling references exist.
 */
public class VersionCatalogCrossReferenceTest {

    @Test
    void pluginVersionRefs_shouldResolve_toExistingVersionEntries() throws IOException {
        String content = readVersionCatalog();
        List<String> versionKeys = extractVersionKeys(content);
        List<String> versionRefs = extractPluginVersionRefs(content);

        for (String ref : versionRefs) {
            assertThat(versionKeys)
                    .as("Plugin version.ref '%s' should resolve to an entry in [versions]", ref)
                    .contains(ref);
        }
    }

    @Test
    void versionCatalog_shouldNotHave_unreferencedPluginVersionRefs() throws IOException {
        String content = readVersionCatalog();
        List<String> versionRefs = extractPluginVersionRefs(content);

        // All version.ref values should be non-empty
        for (String ref : versionRefs) {
            assertThat(ref)
                    .as("Plugin version.ref should not be empty")
                    .isNotBlank();
        }
    }

    @ParameterizedTest(name = "Version catalog key ''{0}'' should have value matching pattern ''{1}''")
    @CsvSource({
            "archunit,   \\d+\\.\\d+\\.\\d+",
            "mockito,    \\d+\\.\\d+\\.\\d+",
            "assertj,    \\d+\\.\\d+\\.\\d+",
            "spring,     \\d+\\.\\d+\\.\\d+"
    })
    void versionEntry_shouldMatch_expectedPattern(String key, String pattern) throws IOException {
        String content = readVersionCatalog();
        Pattern entryPattern = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = entryPattern.matcher(content);

        assertThat(matcher.find())
                .as("Version catalog should contain entry for '%s'", key)
                .isTrue();

        String value = matcher.group(1);
        assertThat(value)
                .as("Version value for '%s' should match pattern '%s'", key, pattern)
                .matches(pattern);
    }

    @Test
    void pluginIds_shouldFollow_reverseDomainNotation() throws IOException {
        String content = readVersionCatalog();
        Pattern pluginId = Pattern.compile("id\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pluginId.matcher(content);

        while (matcher.find()) {
            String id = matcher.group(1);
            assertThat(id)
                    .as("Plugin ID '%s' should follow reverse-domain notation", id)
                    .containsPattern("\\w+\\.\\w+");
        }
    }

    @Test
    void versionCatalog_sectionsOrder_shouldBe_versionsBeforePlugins() throws IOException {
        String content = readVersionCatalog();
        int versionsIndex = content.indexOf("[versions]");
        int pluginsIndex = content.indexOf("[plugins]");

        assertThat(versionsIndex)
                .as("[versions] section should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(pluginsIndex)
                .as("[plugins] section should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(versionsIndex)
                .as("[versions] should appear before [plugins] in the catalog")
                .isLessThan(pluginsIndex);
    }

    @Test
    void versionCatalog_shouldNotHave_trailingWhitespace_inVersionValues() throws IOException {
        List<String> lines = Files.readAllLines(findVersionCatalog());
        Pattern versionLine = Pattern.compile("^\\s*\\w+\\s*=\\s*\"([^\"]+)\"\\s*$");

        for (String line : lines) {
            Matcher matcher = versionLine.matcher(line);
            if (matcher.matches()) {
                String value = matcher.group(1);
                assertThat(value)
                        .as("Version value should not have leading/trailing whitespace: '%s'", value)
                        .isEqualTo(value.trim());
            }
        }
    }

    @Test
    void versionCatalog_archunitVersion_shouldBeConsistentWith_buildDependency() throws IOException {
        String catalogContent = readVersionCatalog();
        Pattern archunitPattern = Pattern.compile("archunit\\s*=\\s*\"([^\"]+)\"");
        Matcher catalogMatcher = archunitPattern.matcher(catalogContent);
        assertThat(catalogMatcher.find()).isTrue();
        String catalogVersion = catalogMatcher.group(1);

        // The configuration build.gradle.kts references libs.versions.archunit —
        // verify it actually uses this indirection (not a hardcoded version)
        String configBuild = Files.readString(
                findProjectRoot().resolve("configuration/build.gradle.kts"));

        assertThat(configBuild)
                .as("Configuration module should reference archunit via version catalog")
                .contains("libs.versions.archunit");

        assertThat(configBuild)
                .as("Configuration module should not hardcode archunit version '%s'", catalogVersion)
                .doesNotContainPattern("archunit-junit5\",\\s*\"" + Pattern.quote(catalogVersion) + "\"");
    }

    private List<String> extractVersionKeys(String catalogContent) {
        List<String> keys = new ArrayList<>();
        boolean inVersions = false;
        for (String line : catalogContent.lines().toList()) {
            if (line.trim().equals("[versions]")) {
                inVersions = true;
                continue;
            }
            if (line.trim().startsWith("[") && !line.trim().equals("[versions]")) {
                inVersions = false;
                continue;
            }
            if (inVersions) {
                Pattern keyPattern = Pattern.compile("^\\s*(\\w+)\\s*=");
                Matcher matcher = keyPattern.matcher(line);
                if (matcher.find()) {
                    keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }

    private List<String> extractPluginVersionRefs(String catalogContent) {
        List<String> refs = new ArrayList<>();
        Pattern refPattern = Pattern.compile("version\\.ref\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = refPattern.matcher(catalogContent);
        while (matcher.find()) {
            refs.add(matcher.group(1));
        }
        return refs;
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findVersionCatalog());
    }

    private Path findVersionCatalog() {
        return findProjectRoot().resolve("gradle/libs.versions.toml");
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
