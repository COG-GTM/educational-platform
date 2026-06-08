package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the end-to-end reference chain from the Gradle version catalog
 * through the build files to the resolved classpath.
 * <p>
 * The Java 26 upgrade added three new test dependencies in
 * configuration/build.gradle.kts, two of which reference the version catalog
 * via {@code libs.versions.<key>.get()}. This test verifies:
 * <ul>
 *   <li>Each catalog-managed dependency references the correct catalog key</li>
 *   <li>The catalog key resolves to a valid version string</li>
 *   <li>The build file uses the correct artifact coordinates</li>
 *   <li>BOM-managed dependencies (without explicit versions) are not accidentally
 *       given catalog versions</li>
 * </ul>
 * <p>
 * {@link TestDependencyScopeValidationTest} verifies scope correctness.
 * {@link VersionCatalogEntryCompletenessTest} verifies catalog entry existence.
 * This test verifies the <em>wiring</em> between catalog and build file.
 */
public class VersionCatalogBuildFileReferenceChainTest {

    @ParameterizedTest(name = "Catalog key ''{0}'' should wire to artifact ''{1}'' in build file")
    @CsvSource({
            "mockito,  mockito-junit-jupiter",
            "assertj,  assertj-core",
            "archunit, archunit-junit5"
    })
    void catalogKey_shouldBeReferenced_inBuildFile(String catalogKey, String artifactId) throws IOException {
        String buildContent = readConfigBuildGradle();
        String catalogContent = readVersionCatalog();

        // Verify catalog has the key
        assertThat(catalogContent)
                .as("Version catalog should define key '%s'", catalogKey)
                .containsPattern(catalogKey + "\\s*=\\s*\"");

        // Verify build file references the catalog key for this artifact
        String expectedRef = "libs.versions." + catalogKey + ".get()";
        assertThat(buildContent)
                .as("Build file should reference '%s' for artifact '%s'", expectedRef, artifactId)
                .contains(artifactId)
                .contains(expectedRef);
    }

    @ParameterizedTest(name = "BOM-managed dependency ''{0}'' should NOT have explicit version")
    @CsvSource({
            "junit-jupiter-api",
            "junit-jupiter-params",
            "junit-platform-engine",
            "junit-platform-launcher"
    })
    void bomManagedDependency_shouldNotHave_explicitVersion(String artifactId) throws IOException {
        String buildContent = readConfigBuildGradle();
        String[] lines = buildContent.split("\n");

        for (String line : lines) {
            if (line.contains(artifactId)) {
                assertThat(line)
                        .as("BOM-managed dependency '%s' should not specify an explicit version", artifactId)
                        .doesNotContain("libs.versions")
                        .doesNotContainPattern("\"\\d+\\.\\d+");
            }
        }
    }

    @Test
    void jupiterEngine_shouldBe_bomManaged_withoutExplicitVersion() throws IOException {
        String buildContent = readConfigBuildGradle();
        String[] lines = buildContent.split("\n");

        for (String line : lines) {
            if (line.contains("junit-jupiter-engine")) {
                assertThat(line)
                        .as("junit-jupiter-engine should be BOM-managed without explicit version")
                        .doesNotContain("libs.versions");
            }
        }
    }

    @Test
    void catalogVersionValues_shouldBe_validSemver() throws IOException {
        String catalogContent = readVersionCatalog();
        Pattern entryPattern = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = entryPattern.matcher(catalogContent);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            // Skip non-version entries (plugin IDs, etc.)
            if (value.contains(".") && !value.contains("-management")) {
                assertThat(value)
                        .as("Catalog entry '%s' value '%s' should be valid semver", key, value)
                        .matches("\\d+\\.\\d+(\\.\\d+)?");
            }
        }
    }

    @Test
    void assertjCatalogVersion_shouldMatch_resolvedVersion() throws IOException {
        String catalogVersion = extractCatalogVersion("assertj");

        // Verify the resolved classpath version matches catalog
        String pomPropsPath = "META-INF/maven/org.assertj/assertj-core/pom.properties";
        var is = getClass().getClassLoader().getResourceAsStream(pomPropsPath);
        if (is != null) {
            var props = new java.util.Properties();
            props.load(is);
            is.close();

            assertThat(props.getProperty("version"))
                    .as("Resolved assertj-core version should match catalog '%s'", catalogVersion)
                    .startsWith(catalogVersion);
        }
    }

    @Test
    void archunitCatalogVersion_shouldMatch_resolvedVersion() throws IOException {
        String catalogVersion = extractCatalogVersion("archunit");

        String pomPropsPath = "META-INF/maven/com.tngtech.archunit/archunit/pom.properties";
        var is = getClass().getClassLoader().getResourceAsStream(pomPropsPath);
        if (is != null) {
            var props = new java.util.Properties();
            props.load(is);
            is.close();

            assertThat(props.getProperty("version"))
                    .as("Resolved archunit version should match catalog '%s'", catalogVersion)
                    .startsWith(catalogVersion);
        }
    }

    private String extractCatalogVersion(String key) throws IOException {
        String content = readVersionCatalog();
        Pattern pattern = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).as("Catalog key '%s' should exist", key).isTrue();
        return matcher.group(1);
    }

    private String readConfigBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
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
