package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

/**
 * Cross-validates that the Mockito version declared in the Gradle version
 * catalog matches the version actually resolved on the test classpath.
 * <p>
 * This mirrors the pattern established by {@link ArchUnitResolvedVersionConsistencyTest}
 * for the ArchUnit dependency. Both tests guard against version drift caused
 * by Gradle's dependency resolution (transitive upgrades, BOM overrides, or
 * resolution strategies) silently changing the version that ends up on the
 * classpath.
 * <p>
 * {@link MockitoMinVersionValidationTest} verifies the classpath version is
 * >= 5.19.0 and checks {@code resolved >= catalog}. This test validates that
 * the resolved version is not <em>downgraded</em> below the catalog floor,
 * stays within the same major version series, and detects when the Spring Boot
 * BOM resolves a different version than what the catalog declares — which is
 * acceptable but worth documenting.
 */
public class MockitoResolvedVersionConsistencyTest {

    @Test
    void resolvedMockitoVersion_shouldBeAtLeast_catalogDeclaration() throws IOException {
        String catalogVersion = extractCatalogVersion("mockito");
        String resolvedVersion = resolveClasspathVersion();

        assertThat(resolvedVersion)
                .as("Resolved Mockito version should be resolvable")
                .isNotNull();

        int comparison = compareVersions(resolvedVersion, catalogVersion);
        assertThat(comparison)
                .as("Resolved Mockito '%s' should not be downgraded below catalog '%s'",
                        resolvedVersion, catalogVersion)
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void catalogMockitoVersion_shouldBe_5_19_0() throws IOException {
        String catalogVersion = extractCatalogVersion("mockito");

        assertThat(catalogVersion)
                .as("Version catalog should declare mockito = 5.19.0")
                .isEqualTo("5.19.0");
    }

    @Test
    void resolvedMockitoVersion_shouldBe_sameMajorVersion_asCatalog() throws IOException {
        String resolvedVersion = resolveClasspathVersion();
        String catalogVersion = extractCatalogVersion("mockito");

        assertThat(resolvedVersion).isNotNull();

        Pattern semver = Pattern.compile("(\\d+)\\.\\d+\\.\\d+");
        Matcher resolvedMatcher = semver.matcher(resolvedVersion);
        Matcher catalogMatcher = semver.matcher(catalogVersion);
        assertThat(resolvedMatcher.find()).isTrue();
        assertThat(catalogMatcher.find()).isTrue();

        assertThat(resolvedMatcher.group(1))
                .as("Resolved Mockito '%s' should be same major version as catalog '%s'",
                        resolvedVersion, catalogVersion)
                .isEqualTo(catalogMatcher.group(1));
    }

    @Test
    void mockitoJar_shouldBeLoaded_fromExpectedCoordinates() {
        URL location = Mockito.class.getProtectionDomain().getCodeSource().getLocation();

        assertThat(location)
                .as("Mockito class should have a known code source location")
                .isNotNull();

        assertThat(location.toString())
                .as("Mockito JAR path should reference mockito-core")
                .containsIgnoringCase("mockito");
    }

    @Test
    void mockitoJupiterExtension_shouldBeLoaded_fromExpectedCoordinates() {
        try {
            Class<?> extensionClass = Class.forName("org.mockito.junit.jupiter.MockitoExtension");
            URL location = extensionClass.getProtectionDomain().getCodeSource().getLocation();

            assertThat(location)
                    .as("MockitoExtension should have a known code source location")
                    .isNotNull();

            assertThat(location.toString())
                    .as("MockitoExtension JAR path should reference mockito-junit-jupiter")
                    .containsIgnoringCase("mockito");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("MockitoExtension should be loadable", e);
        }
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
        String pomPropsPath = "META-INF/maven/org.mockito/mockito-core/pom.properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        try {
            URL location = Mockito.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.getPath();
            Pattern jarVersion = Pattern.compile("mockito-core-(\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = jarVersion.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        Package pkg = Mockito.class.getPackage();
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
