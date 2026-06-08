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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Programmatically validates that the Mockito library version on the classpath
 * is >= 5.19.0, the minimum required for reliable Java 26 byte-buddy proxy generation.
 * <p>
 * Mockito uses byte-buddy internally to generate subclass/interface proxies at
 * runtime. Byte-buddy must be able to parse Java 26 class files (major version 70)
 * to create mocks and spies. Mockito 5.19.0 bundles a byte-buddy version that
 * supports class file major version 70.
 * <p>
 * {@link MockitoJpaProxyJava26Test} verifies mocking <em>works</em>. This test
 * verifies the <em>version</em> is correct, guarding against accidental downgrades
 * that would cause hard-to-diagnose proxy generation failures.
 */
public class MockitoMinVersionValidationTest {

    private static final int MIN_MAJOR = 5;
    private static final int MIN_MINOR = 19;
    private static final int MIN_PATCH = 0;

    @Test
    void mockitoVersion_shouldBeAtLeast_5_19_0() {
        String version = resolveMockitoVersion();

        assertThat(version)
                .as("Mockito version should be resolvable from classpath")
                .isNotNull();

        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = semver.matcher(version);
        assertThat(matcher.find())
                .as("Mockito version '%s' should be parseable as semver", version)
                .isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        boolean meetsMinimum = (major > MIN_MAJOR)
                || (major == MIN_MAJOR && minor > MIN_MINOR)
                || (major == MIN_MAJOR && minor == MIN_MINOR && patch >= MIN_PATCH);

        assertThat(meetsMinimum)
                .as("Mockito %d.%d.%d should be >= %d.%d.%d for Java 26 byte-buddy support",
                        major, minor, patch, MIN_MAJOR, MIN_MINOR, MIN_PATCH)
                .isTrue();
    }

    @Test
    void mockitoVersion_shouldNotBe_priorMajor() {
        String version = resolveMockitoVersion();

        assertThat(version)
                .as("Mockito version should not be Mockito 4.x (incompatible with Java 26)")
                .doesNotStartWith("4.");
    }

    @Test
    void mockitoVersion_onClasspath_shouldBeAtLeast_versionCatalogDeclaration() throws IOException {
        String resolvedVersion = resolveMockitoVersion();
        String catalogVersion = readCatalogMockitoVersion();

        assertThat(resolvedVersion)
                .as("Resolved Mockito version should not be null")
                .isNotNull();

        // The Spring Boot BOM may resolve a higher version than declared in the catalog.
        // Verify resolved >= catalog (not necessarily equal).
        assertThat(compareVersions(resolvedVersion, catalogVersion))
                .as("Resolved Mockito version '%s' should be >= catalog declaration '%s'",
                        resolvedVersion, catalogVersion)
                .isGreaterThanOrEqualTo(0);
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

    @Test
    void mockito_coreClass_shouldBe_loadable() {
        assertThatCode(() -> {
            Class<?> mockitoClass = Class.forName("org.mockito.Mockito");
            assertThat(mockitoClass).isNotNull();
        }).as("Mockito core class should be loadable on the classpath")
                .doesNotThrowAnyException();
    }

    @Test
    void mockito_byteBuddy_shouldBe_available() {
        assertThatCode(() -> {
            Class<?> bbClass = Class.forName("net.bytebuddy.ByteBuddy");
            assertThat(bbClass).isNotNull();
        }).as("ByteBuddy (bundled with Mockito) should be available for Java 26 proxy generation")
                .doesNotThrowAnyException();
    }

    @Test
    void mockito_shouldCreate_simpleProxy_onJava26() {
        assertThatCode(() -> {
            Runnable mock = Mockito.mock(Runnable.class);
            mock.run();
            Mockito.verify(mock).run();
        }).as("Basic Mockito proxy creation should work on Java 26 runtime")
                .doesNotThrowAnyException();
    }

    private String resolveMockitoVersion() {
        // Try reading from Mockito JAR's pom.properties
        String pomPropsPath = "META-INF/maven/org.mockito/mockito-core/pom.properties";
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
            URL location = Mockito.class.getProtectionDomain().getCodeSource().getLocation();
            String path = location.getPath();
            Pattern jarVersion = Pattern.compile("mockito-core[^/]*?(\\d+\\.\\d+\\.\\d+)");
            Matcher matcher = jarVersion.matcher(path);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        // Fallback: Package specification version
        Package pkg = Mockito.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }

        return null;
    }

    private String readCatalogMockitoVersion() throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);
        Pattern pattern = Pattern.compile("mockito\\s*=\\s*\"(\\d+\\.\\d+\\.\\d+)\"");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).as("Should find mockito version in catalog").isTrue();
        return matcher.group(1);
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            if (Files.exists(current.resolve("build.gradle.kts"))
                    && Files.exists(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(".");
    }
}
