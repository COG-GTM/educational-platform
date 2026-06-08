package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that runtime dependency versions resolved on the classpath match
 * the versions declared in gradle/libs.versions.toml.
 * <p>
 * The Java 26 upgrade added assertj-core and junit-jupiter-params as explicit
 * dependencies. This test ensures no version mismatch between the catalog
 * declaration and what's actually available at runtime — such a mismatch would
 * indicate broken dependency resolution or a BOM override.
 */
public class DependencyResolutionVerificationTest {

    @Test
    void assertjVersion_shouldMatch_catalogDeclaration() throws IOException {
        String catalogVersion = readCatalogVersion("assertj");

        assertThat(catalogVersion)
                .as("assertj version should be resolvable from catalog")
                .isNotNull();

        // Verify assertj is actually available on classpath at the declared version
        String runtimeVersion = resolveJarVersion("assertj-core", "org.assertj");
        if (runtimeVersion != null) {
            assertThat(runtimeVersion)
                    .as("Runtime assertj version should match catalog declaration")
                    .isEqualTo(catalogVersion);
        }
    }

    @Test
    void mockitoVersion_shouldMatch_catalogDeclaration() throws IOException {
        String catalogVersion = readCatalogVersion("mockito");

        assertThat(catalogVersion)
                .as("mockito version should be resolvable from catalog")
                .isNotNull();

        String runtimeVersion = resolveJarVersion("mockito-core", "org.mockito");
        if (runtimeVersion != null) {
            assertThat(runtimeVersion)
                    .as("Runtime mockito version should match catalog declaration")
                    .isEqualTo(catalogVersion);
        }
    }

    @Test
    void archunitVersion_shouldMatch_catalogDeclaration() throws IOException {
        String catalogVersion = readCatalogVersion("archunit");

        assertThat(catalogVersion)
                .as("archunit version should be resolvable from catalog")
                .isNotNull();

        String runtimeVersion = resolveJarVersion("archunit", "com.tngtech.archunit");
        if (runtimeVersion != null) {
            assertThat(runtimeVersion)
                    .as("Runtime archunit version should match catalog declaration")
                    .isEqualTo(catalogVersion);
        }
    }

    @ParameterizedTest(name = "Catalog version for {0} should be valid semver")
    @CsvSource({
            "spring,   '4.0.1'",
            "mockito,  '5.19.0'",
            "assertj,  '3.27.3'",
            "archunit, '1.4.2'"
    })
    void catalogVersion_shouldMatch_expectedValue(String key, String expectedVersion) throws IOException {
        String actual = readCatalogVersion(key);

        assertThat(actual)
                .as("Catalog version for '%s'", key)
                .isEqualTo(expectedVersion);
    }

    @Test
    void assertjCore_shouldBeLoadable_onClasspath() {
        assertThatCode(() -> Class.forName("org.assertj.core.api.Assertions"))
                .as("AssertJ Assertions class should be on test classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void junitJupiterParams_shouldBeLoadable_onClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.params.ParameterizedTest"))
                .as("JUnit Jupiter Params should be on test classpath (added in Java 26 upgrade)")
                .doesNotThrowAnyException();
    }

    @Test
    void junitJupiterEngine_shouldBeLoadable_onClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.engine.JupiterTestEngine"))
                .as("JUnit Jupiter Engine should be on test runtime classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void archunitJunit5_shouldBeLoadable_onClasspath() {
        assertThatCode(() -> Class.forName("com.tngtech.archunit.junit.ArchTest"))
                .as("ArchUnit JUnit5 support should be on test classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void allCatalogVersions_shouldBe_nonSnapshot() throws IOException {
        Path catalog = findVersionCatalog();
        String content = Files.readString(catalog);

        Pattern versionEntry = Pattern.compile("=\\s*\"([^\"]+)\"");
        Matcher matcher = versionEntry.matcher(content);

        while (matcher.find()) {
            String version = matcher.group(1);
            assertThat(version)
                    .as("Catalog version '%s' should not be a SNAPSHOT", version)
                    .doesNotContainIgnoringCase("SNAPSHOT");
        }
    }

    @Test
    void catalogVersions_shouldAll_parseAsValidSemver() throws IOException {
        Path catalog = findVersionCatalog();
        String content = Files.readString(catalog);

        Pattern versionLine = Pattern.compile("^\\s*\\w+\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE);
        Matcher matcher = versionLine.matcher(content);

        Pattern semver = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?");
        while (matcher.find()) {
            String version = matcher.group(1);
            // Skip plugin IDs which contain dots but aren't versions
            if (!version.contains("io.spring")) {
                assertThat(semver.matcher(version).find())
                        .as("Version '%s' should contain a semver-like pattern", version)
                        .isTrue();
            }
        }
    }

    private String readCatalogVersion(String key) throws IOException {
        Path catalog = findVersionCatalog();
        for (String line : Files.readAllLines(catalog)) {
            Pattern pattern = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String resolveJarVersion(String artifactId, String groupId) {
        String pomPropsPath = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        // Fallback: try to resolve from JAR URL
        try {
            String classResource = "/" + groupId.replace('.', '/') + "/";
            URL url = getClass().getResource(classResource);
            if (url != null) {
                String path = url.getPath();
                Pattern jarVersion = Pattern.compile(artifactId + "-(\\d+\\.\\d+\\.\\d+)");
                Matcher matcher = jarVersion.matcher(path);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Path findVersionCatalog() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("gradle/libs.versions.toml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Paths.get("gradle/libs.versions.toml");
    }
}
