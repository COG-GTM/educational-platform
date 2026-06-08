package com.educational.platform.java;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Programmatically validates that the ArchUnit library version on the classpath
 * is >= 1.4.2, the minimum required for Java 26 (class file major version 70).
 * <p>
 * This guards against accidental downgrades of ArchUnit that would cause
 * all architecture tests to fail with "Unsupported class file major version 70".
 */
public class ArchUnitMinVersionValidationTest {

    private static final int MIN_MAJOR = 1;
    private static final int MIN_MINOR = 4;
    private static final int MIN_PATCH = 2;

    @Test
    void archUnitVersion_shouldBeAtLeast_142() {
        String version = resolveArchUnitVersion();

        assertThat(version)
                .as("ArchUnit version should be resolvable from classpath")
                .isNotNull();

        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = semver.matcher(version);
        assertThat(matcher.find())
                .as("ArchUnit version '%s' should be parseable as semver", version)
                .isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        boolean meetsMinimum = (major > MIN_MAJOR)
                || (major == MIN_MAJOR && minor > MIN_MINOR)
                || (major == MIN_MAJOR && minor == MIN_MINOR && patch >= MIN_PATCH);

        assertThat(meetsMinimum)
                .as("ArchUnit %d.%d.%d should be >= %d.%d.%d for Java 26 support",
                        major, minor, patch, MIN_MAJOR, MIN_MINOR, MIN_PATCH)
                .isTrue();
    }

    @Test
    void archUnitVersion_shouldNotBe_141() {
        String version = resolveArchUnitVersion();

        assertThat(version)
                .as("ArchUnit version should not be the pre-upgrade 1.4.1")
                .doesNotStartWith("1.4.1");
    }

    @Test
    void archUnit_classFileImporter_shouldBe_loadable() {
        assertThatCode(() -> {
            var importer = new ClassFileImporter();
            assertThat(importer).isNotNull();
        }).as("ClassFileImporter should be instantiable with current ArchUnit version")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_configuration_shouldBe_accessible() {
        assertThatCode(() -> {
            ArchConfiguration config = ArchConfiguration.get();
            assertThat(config).isNotNull();
        }).as("ArchConfiguration should be accessible")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_asmJar_shouldSupportMajorVersion70() {
        assertThatCode(() -> {
            var classes = new ClassFileImporter().importClasses(ArchUnitMinVersionValidationTest.class);
            assertThat(classes).isNotEmpty();
        }).as("ArchUnit's bundled ASM should handle this test class compiled with Java 26 (major version 70)")
                .doesNotThrowAnyException();
    }

    private String resolveArchUnitVersion() {
        // Try reading from the ArchUnit JAR's pom.properties
        String pomPropsPath = "META-INF/maven/com.tngtech.archunit/archunit/pom.properties";
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
            URL location = ClassFileImporter.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.getPath();
            Pattern jarVersion = Pattern.compile("archunit[^/]*?(\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = jarVersion.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        // Fallback: check Package specification version
        Package pkg = ClassFileImporter.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }

        return null;
    }
}
