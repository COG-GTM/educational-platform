package com.educational.platform.java;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
 * Cross-validates that the AssertJ version declared in the Gradle version
 * catalog matches the version actually resolved on the test classpath.
 * <p>
 * {@link VersionCatalogBuildFileReferenceChainTest} contains a single test
 * {@code assertjCatalogVersion_shouldMatch_resolvedVersion} that checks the
 * catalog-to-classpath chain. This dedicated test class provides deeper
 * validation:
 * <ul>
 *   <li>Exact catalog vs classpath version match (not just presence)</li>
 *   <li>Detection of BOM override: Spring Boot BOM also manages AssertJ,
 *       so an explicit catalog version could silently diverge from the
 *       BOM-resolved version if the catalog lags behind</li>
 *   <li>JAR coordinate verification to confirm the correct artifact is loaded</li>
 *   <li>Functional verification that the resolved version supports Java 26</li>
 * </ul>
 * <p>
 * {@link AssertJMinVersionValidationTest} verifies the minimum version
 * is >= 3.27. This test verifies the <em>exact</em> version matches what
 * the catalog declares.
 */
public class AssertJResolvedVersionConsistencyTest {

    @Test
    void resolvedAssertJVersion_shouldMatch_catalogDeclaration() throws IOException {
        String catalogVersion = extractCatalogVersion("assertj");
        String resolvedVersion = resolveClasspathVersion();

        assertThat(resolvedVersion)
                .as("Resolved AssertJ version on classpath should match catalog declaration '%s'",
                        catalogVersion)
                .isNotNull()
                .startsWith(catalogVersion);
    }

    @Test
    void catalogAssertJVersion_shouldBe_3_27_3() throws IOException {
        String catalogVersion = extractCatalogVersion("assertj");

        assertThat(catalogVersion)
                .as("Version catalog should declare assertj = 3.27.3")
                .isEqualTo("3.27.3");
    }

    @Test
    void resolvedAssertJVersion_shouldNotBe_olderThanCatalog() throws IOException {
        String resolvedVersion = resolveClasspathVersion();
        String catalogVersion = extractCatalogVersion("assertj");

        assertThat(resolvedVersion).isNotNull();

        int comparison = compareVersions(resolvedVersion, catalogVersion);
        assertThat(comparison)
                .as("Resolved AssertJ '%s' should not be older than catalog '%s' (possible BOM downgrade)",
                        resolvedVersion, catalogVersion)
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void assertJJar_shouldBeLoaded_fromExpectedCoordinates() {
        URL location = Assertions.class.getProtectionDomain().getCodeSource().getLocation();

        assertThat(location)
                .as("AssertJ Assertions should have a known code source location")
                .isNotNull();

        assertThat(location.toString())
                .as("AssertJ JAR path should reference assertj-core")
                .containsIgnoringCase("assertj-core");
    }

    @Test
    void assertJ_shouldSupportJava26_functionally() {
        assertThatCode(() -> {
            // Verify core assertion APIs work on Java 26 bytecode
            assertThat("Java 26").startsWith("Java").endsWith("26");
            assertThat(java.util.List.of(1, 2, 3)).hasSize(3).contains(2);
            assertThat(java.util.Optional.of("value")).isPresent().contains("value");
            assertThat(java.util.Optional.empty()).isEmpty();
        }).as("AssertJ core APIs should function correctly on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void assertJ_softAssertions_shouldWork_onJava26() {
        assertThatCode(() -> {
            org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
            softly.assertThat(Runtime.version().feature()).isEqualTo(26);
            softly.assertThat("70.0").isEqualTo(System.getProperty("java.class.version"));
            softly.assertAll();
        }).as("AssertJ SoftAssertions (proxy-based) should work on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    private String extractCatalogVersion(String key) throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);
        Pattern pattern = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find())
                .as("Version catalog should contain entry for '%s'", key)
                .isTrue();
        return matcher.group(1);
    }

    private String resolveClasspathVersion() {
        String pomPropsPath = "META-INF/maven/org.assertj/assertj-core/pom.properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        try {
            URL location = Assertions.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.getPath();
            Pattern jarVersion = Pattern.compile("assertj-core-(\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = jarVersion.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        Package pkg = Assertions.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return null;
    }

    private int compareVersions(String v1, String v2) {
        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher m1 = semver.matcher(v1);
        Matcher m2 = semver.matcher(v2);
        if (!m1.find() || !m2.find()) return 0;
        int major = Integer.compare(Integer.parseInt(m1.group(1)), Integer.parseInt(m2.group(1)));
        if (major != 0) return major;
        int minor = Integer.compare(Integer.parseInt(m1.group(2)), Integer.parseInt(m2.group(2)));
        if (minor != 0) return minor;
        return Integer.compare(Integer.parseInt(m1.group(3)), Integer.parseInt(m2.group(3)));
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
