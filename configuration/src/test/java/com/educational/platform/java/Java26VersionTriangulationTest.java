package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Triangulates the Java 26 version number across five independent sources
 * in a single test to detect any inconsistency.
 * <p>
 * Individual tests validate each source in isolation:
 * <ul>
 *   <li>{@link JavaVersionTest} — runtime version, bytecode version, system properties</li>
 *   <li>{@link BuildConfigVersionsTest} — build.gradle.kts VERSION_26</li>
 *   <li>{@link ReadmeVersionTest} — README.md Java 26 references</li>
 *   <li>{@link GradleWrapperConfigTest} — Gradle version >= 9.4 for Java 26</li>
 * </ul>
 * <p>
 * However, isolated tests cannot detect <em>mutual inconsistency</em> between
 * sources. For example, if the build targets VERSION_26 but the runtime is
 * Java 25, each individual test might pass (build file contains "26", runtime
 * is >= some threshold) while the overall system is incoherent. This test
 * extracts the version number from all five sources and asserts they all
 * agree on the same value: 26.
 */
public class Java26VersionTriangulationTest {

    private static final int EXPECTED_JAVA_VERSION = 26;
    private static final int EXPECTED_CLASS_FILE_MAJOR = 70; // 44 + 26

    @Test
    void allFiveVersionSources_shouldAgree_onJava26() throws IOException {
        // Source 1: Runtime feature version
        int runtimeVersion = Runtime.version().feature();

        // Source 2: java.specification.version system property
        int specVersion = Integer.parseInt(System.getProperty("java.specification.version"));

        // Source 3: Class file major version (bytecode)
        int classFileMajor = readClassFileMajorVersion();
        int derivedJavaVersion = classFileMajor - 44;

        // Source 4: build.gradle.kts VERSION_N constant
        int buildVersion = extractBuildJavaVersion();

        // Source 5: README.md Java N reference
        int readmeVersion = extractReadmeJavaVersion();

        // All five sources must agree
        assertThat(runtimeVersion)
                .as("Runtime.version().feature()")
                .isEqualTo(EXPECTED_JAVA_VERSION);

        assertThat(specVersion)
                .as("java.specification.version")
                .isEqualTo(EXPECTED_JAVA_VERSION);

        assertThat(derivedJavaVersion)
                .as("Class file major version %d → Java %d", classFileMajor, derivedJavaVersion)
                .isEqualTo(EXPECTED_JAVA_VERSION);

        assertThat(buildVersion)
                .as("build.gradle.kts VERSION_N")
                .isEqualTo(EXPECTED_JAVA_VERSION);

        assertThat(readmeVersion)
                .as("README.md Java version reference")
                .isEqualTo(EXPECTED_JAVA_VERSION);

        // Cross-validate the class file formula
        assertThat(classFileMajor)
                .as("Class file major version should equal 44 + runtime feature version")
                .isEqualTo(44 + runtimeVersion);

        // Cross-validate java.class.version system property
        String classVersionProp = System.getProperty("java.class.version");
        assertThat(classVersionProp)
                .as("java.class.version should be '%d.0'", EXPECTED_CLASS_FILE_MAJOR)
                .isEqualTo(EXPECTED_CLASS_FILE_MAJOR + ".0");
    }

    @Test
    void gradleVersion_shouldBeCompatible_withTriangulatedJavaVersion() throws IOException {
        int javaVersion = Runtime.version().feature();

        // Gradle compatibility: Java 26 requires Gradle >= 9.4
        Properties wrapperProps = new Properties();
        wrapperProps.load(Files.newInputStream(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = wrapperProps.getProperty("distributionUrl");

        Pattern versionPattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = versionPattern.matcher(distUrl);
        assertThat(matcher.find()).isTrue();

        int gradleMajor = Integer.parseInt(matcher.group(1));
        int gradleMinor = Integer.parseInt(matcher.group(2));

        // Java 26 requires Gradle >= 9.4
        if (javaVersion >= 26) {
            boolean gradleSupportsJava = gradleMajor > 9 || (gradleMajor == 9 && gradleMinor >= 4);
            assertThat(gradleSupportsJava)
                    .as("Gradle %d.%d should support Java %d (requires >= 9.4)",
                            gradleMajor, gradleMinor, javaVersion)
                    .isTrue();
        }
    }

    @Test
    void versionCatalog_archunit_shouldSupportTriangulatedJavaVersion() throws IOException {
        int javaVersion = Runtime.version().feature();

        String catalog = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
        Pattern archunitPattern = Pattern.compile("archunit\\s*=\\s*\"(\\d+)\\.(\\d+)\\.(\\d+)\"");
        Matcher matcher = archunitPattern.matcher(catalog);
        assertThat(matcher.find()).isTrue();

        int archMajor = Integer.parseInt(matcher.group(1));
        int archMinor = Integer.parseInt(matcher.group(2));
        int archPatch = Integer.parseInt(matcher.group(3));

        // Java 26 requires ArchUnit >= 1.4.2
        if (javaVersion >= 26) {
            boolean archunitSupports = archMajor > 1
                    || (archMajor == 1 && archMinor > 4)
                    || (archMajor == 1 && archMinor == 4 && archPatch >= 2);
            assertThat(archunitSupports)
                    .as("ArchUnit %d.%d.%d should support Java %d (requires >= 1.4.2)",
                            archMajor, archMinor, archPatch, javaVersion)
                    .isTrue();
        }
    }

    private int readClassFileMajorVersion() throws IOException {
        String classResource = "/com/educational/platform/java/Java26VersionTriangulationTest.class";
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is).as("Test class resource should be loadable").isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic).isEqualTo(0xCAFEBABE);
            dis.readUnsignedShort(); // minor
            return dis.readUnsignedShort(); // major
        }
    }

    private int extractBuildJavaVersion() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));
        Pattern pattern = Pattern.compile("VERSION_(\\d+)");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    private int extractReadmeJavaVersion() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("README.md"));
        // Match "Java 26" in the technology stack section
        Pattern pattern = Pattern.compile("Java\\s+(\\d+)");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
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
