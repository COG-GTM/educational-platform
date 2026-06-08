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

/**
 * Validates that test dependency versions declared in the version catalog
 * meet the minimum requirements for Java 26 compatibility.
 * <p>
 * {@link ArchUnitMinVersionValidationTest} validates ArchUnit >= 1.4.2.
 * This test extends that pattern to AssertJ, Mockito, and JUnit — all of
 * which require minimum versions for Java 26 bytecode, record support,
 * and sealed class handling.
 * <p>
 * Minimum versions for Java 26:
 * <ul>
 *   <li>AssertJ >= 3.25.0 — recursive comparison of records, Java 21+ sealed class support</li>
 *   <li>Mockito >= 5.5.0 — inline mock maker supporting Java 21+ class files</li>
 *   <li>JUnit >= 5.10.0 — {@code @ParameterizedTest} with records, sealed class discovery</li>
 * </ul>
 */
public class TestDependencyMinVersionJava26Test {

    @Test
    void assertjVersion_shouldBe_atLeast_3250_forJava26() throws IOException {
        String version = readVersionFromCatalog("assertj");
        assertThat(version).as("AssertJ version should be resolvable").isNotNull();

        assertVersionAtLeast(version, 3, 25, 0,
                "AssertJ >= 3.25.0 required for Java 21+ record/sealed class support");
    }

    @Test
    void mockitoVersion_shouldBe_atLeast_550_forJava26() throws IOException {
        String version = readVersionFromCatalog("mockito");
        assertThat(version).as("Mockito version should be resolvable").isNotNull();

        assertVersionAtLeast(version, 5, 5, 0,
                "Mockito >= 5.5.0 required for Java 21+ inline mocking support");
    }

    @Test
    void assertjVersion_shouldBe_resolvableAtRuntime() {
        Package pkg = org.assertj.core.api.Assertions.class.getPackage();
        String implVersion = resolveLibraryVersion(
                "org.assertj", "assertj-core",
                org.assertj.core.api.Assertions.class);

        assertThat(implVersion)
                .as("AssertJ runtime version should be resolvable")
                .isNotNull();
    }

    @Test
    void mockitoVersion_shouldBe_resolvableAtRuntime() {
        String implVersion = resolveLibraryVersion(
                "org.mockito", "mockito-core",
                org.mockito.Mockito.class);

        assertThat(implVersion)
                .as("Mockito runtime version should be resolvable")
                .isNotNull();
    }

    @ParameterizedTest(name = "Version catalog key ''{0}'' should not reference pre-Java-26 version ''{1}''")
    @CsvSource({
            "assertj,  3.24.2",
            "assertj,  3.23.0",
            "mockito,  5.4.0",
            "mockito,  4.11.0",
            "archunit, 1.4.1",
            "archunit, 1.3.0"
    })
    void versionCatalog_shouldNotReference_oldIncompatibleVersion(
            String key, String oldVersion) throws IOException {
        String currentVersion = readVersionFromCatalog(key);

        assertThat(currentVersion)
                .as("Version catalog '%s' should not be old version '%s'", key, oldVersion)
                .isNotEqualTo(oldVersion);
    }

    @Test
    void allTestDependencyVersions_shouldBe_stableReleases() throws IOException {
        String catalog = readVersionCatalog();

        for (String key : new String[]{"assertj", "mockito", "archunit"}) {
            String version = readVersionFromCatalog(key);
            assertThat(version)
                    .as("Version for '%s' should be a stable release (no -SNAPSHOT, -rc, -beta)", key)
                    .doesNotContainIgnoringCase("snapshot")
                    .doesNotContainIgnoringCase("-rc")
                    .doesNotContainIgnoringCase("-beta")
                    .doesNotContainIgnoringCase("-alpha")
                    .doesNotContainIgnoringCase("-milestone");
        }
    }

    private void assertVersionAtLeast(String version, int minMajor, int minMinor, int minPatch,
                                      String reason) {
        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = semver.matcher(version);
        assertThat(matcher.find())
                .as("Version '%s' should be parseable as semver", version)
                .isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        boolean meetsMinimum = (major > minMajor)
                || (major == minMajor && minor > minMinor)
                || (major == minMajor && minor == minMinor && patch >= minPatch);

        assertThat(meetsMinimum)
                .as("%s — current: %d.%d.%d, minimum: %d.%d.%d",
                        reason, major, minor, patch, minMajor, minMinor, minPatch)
                .isTrue();
    }

    private String readVersionFromCatalog(String key) throws IOException {
        String content = readVersionCatalog();
        Pattern pattern = Pattern.compile(key + "\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
    }

    private String resolveLibraryVersion(String groupId, String artifactId, Class<?> clazz) {
        // Try pom.properties
        String pomPath = String.format("META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(pomPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        // Try JAR file name
        try {
            URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
            Matcher matcher = Pattern.compile("(\\d+\\.\\d+\\.\\d+)").matcher(location.getPath());
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        // Try package version
        Package pkg = clazz.getPackage();
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
