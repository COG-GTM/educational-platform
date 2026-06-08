package com.educational.platform.java;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
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
 * Validates that the AssertJ library version on the classpath matches the
 * version declared in the Gradle version catalog and is compatible with Java 26.
 * <p>
 * AssertJ was newly added as an explicit test dependency during the Java 26
 * upgrade. This test guards against:
 * <ul>
 *   <li>Accidental downgrades below the declared catalog version</li>
 *   <li>Mismatch between the catalog declaration and what actually resolves</li>
 *   <li>Broken classpath causing assertion APIs to be unavailable at runtime</li>
 * </ul>
 * <p>
 * {@link DependencyVersionTest} checks basic classpath availability.
 * {@link NewTestDependencyClasspathTest} checks the dependency is declared.
 * This test focuses on <em>version correctness</em> of the resolved AssertJ.
 */
public class AssertJMinVersionValidationTest {

    private static final int MIN_MAJOR = 3;
    private static final int MIN_MINOR = 27;
    private static final int MIN_PATCH = 3;

    @Test
    void assertjVersion_shouldBeAtLeast_3_27_3() {
        String version = resolveAssertJVersion();

        assertThat(version)
                .as("AssertJ version should be resolvable from classpath")
                .isNotNull();

        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = semver.matcher(version);
        assertThat(matcher.find())
                .as("AssertJ version '%s' should be parseable as semver", version)
                .isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        boolean meetsMinimum = (major > MIN_MAJOR)
                || (major == MIN_MAJOR && minor > MIN_MINOR)
                || (major == MIN_MAJOR && minor == MIN_MINOR && patch >= MIN_PATCH);

        assertThat(meetsMinimum)
                .as("AssertJ %d.%d.%d should be >= %d.%d.%d (declared in version catalog)",
                        major, minor, patch, MIN_MAJOR, MIN_MINOR, MIN_PATCH)
                .isTrue();
    }

    @Test
    void assertjVersion_onClasspath_shouldMatch_versionCatalogDeclaration() throws IOException {
        String resolvedVersion = resolveAssertJVersion();
        String catalogVersion = readCatalogAssertJVersion();

        assertThat(resolvedVersion)
                .as("Resolved AssertJ version should match catalog declaration '%s'", catalogVersion)
                .isNotNull()
                .startsWith(catalogVersion);
    }

    @Test
    void assertj_softAssertions_shouldBeUsable_onJava26() {
        assertThatCode(() -> {
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat("hello").isNotNull();
            soft.assertThat(42).isPositive();
            soft.assertAll();
        }).as("SoftAssertions should work on Java 26 bytecode")
                .doesNotThrowAnyException();
    }

    @Test
    void assertj_shouldSupportJava26Records() {
        record TestRecord(String name, int value) {}
        TestRecord r = new TestRecord("test", 42);

        assertThat(r.name()).isEqualTo("test");
        assertThat(r.value()).isEqualTo(42);
        assertThat(r).extracting("name", "value")
                .containsExactly("test", 42);
    }

    @Test
    void assertj_shouldSupportModernCollectionAssertions() {
        var list = java.util.List.of("a", "b", "c");

        assertThat(list)
                .hasSize(3)
                .first().isEqualTo("a");
    }

    private String resolveAssertJVersion() {
        // Try reading from the AssertJ JAR's pom.properties
        String pomPropsPath = "META-INF/maven/org.assertj/assertj-core/pom.properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        // Fallback: resolve from JAR file name on classpath
        try {
            URL location = Assertions.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.getPath();
            Pattern jarVersion = Pattern.compile("assertj-core[^/]*?(\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = jarVersion.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        // Fallback: check Package specification version
        Package pkg = Assertions.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }

        return null;
    }

    private String readCatalogAssertJVersion() throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);
        Pattern pattern = Pattern.compile("assertj\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).as("Version catalog should define assertj version").isTrue();
        return matcher.group(1);
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
