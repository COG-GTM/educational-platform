package com.educational.platform.java;

import com.tngtech.archunit.core.importer.ClassFileImporter;
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

/**
 * Cross-validates that the ArchUnit version declared in the Gradle version
 * catalog matches the version actually resolved on the test classpath.
 * <p>
 * A mismatch can happen if Gradle's dependency resolution picks a different
 * version due to transitive dependency conflicts, BOM overrides, or resolution
 * strategies. This test ensures the {@code archunit = "1.4.2"} declaration in
 * {@code libs.versions.toml} is what actually ends up on the classpath.
 * <p>
 * {@link ArchUnitMinVersionValidationTest} verifies the classpath version is
 * >= 1.4.2. This test verifies it matches <em>exactly</em> what the catalog
 * declares.
 */
public class ArchUnitResolvedVersionConsistencyTest {

    @Test
    void resolvedArchUnitVersion_shouldMatch_catalogDeclaration() throws IOException {
        String catalogVersion = extractCatalogVersion();
        String resolvedVersion = resolveClasspathVersion();

        assertThat(resolvedVersion)
                .as("Resolved ArchUnit version on classpath should match catalog declaration '%s'",
                        catalogVersion)
                .isNotNull()
                .startsWith(catalogVersion);
    }

    @Test
    void catalogArchUnitVersion_shouldBe_142() throws IOException {
        String catalogVersion = extractCatalogVersion();

        assertThat(catalogVersion)
                .as("Version catalog should declare archunit = 1.4.2")
                .isEqualTo("1.4.2");
    }

    @Test
    void resolvedArchUnitVersion_shouldNotBe_141() throws IOException {
        String resolvedVersion = resolveClasspathVersion();

        assertThat(resolvedVersion)
                .as("Resolved ArchUnit version should not be old 1.4.1")
                .doesNotStartWith("1.4.1");
    }

    @Test
    void archUnitJar_shouldBeLoaded_fromExpectedCoordinates() {
        URL location = ClassFileImporter.class.getProtectionDomain().getCodeSource().getLocation();

        assertThat(location)
                .as("ArchUnit ClassFileImporter should have a known code source location")
                .isNotNull();

        assertThat(location.toString())
                .as("ArchUnit JAR path should reference archunit")
                .containsIgnoringCase("archunit");
    }

    private String extractCatalogVersion() throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);
        Pattern pattern = Pattern.compile("archunit\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String resolveClasspathVersion() {
        // Try pom.properties from the JAR
        String pomPropsPath = "META-INF/maven/com.tngtech.archunit/archunit/pom.properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        // Fallback: extract from JAR file name
        try {
            URL location = ClassFileImporter.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.getPath();
            Pattern jarVersion = Pattern.compile("archunit[^/]*?(\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = jarVersion.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        // Fallback: Package spec version
        Package pkg = ClassFileImporter.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return null;
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
