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
 * Composite end-to-end coherence test that validates ALL version touch-points
 * in a single test class, treating the entire upgrade as an atomic unit.
 * <p>
 * Individual tests in {@link VersionConsistencyTest}, {@link BuildConfigVersionsTest},
 * and {@link ReadmeVersionTest} check specific file pairs. This test aggregates
 * all version sources (README, build.gradle.kts, gradle-wrapper.properties,
 * libs.versions.toml, JVM runtime, and compiled bytecode) into a unified
 * "version fingerprint" and validates they all agree.
 * <p>
 * This catches partial upgrades that might pass individual cross-file tests
 * but fail as a whole (e.g., README says Java 26, build says 26, but wrapper
 * still points to Gradle 9.2.1 which only supports up to Java 25).
 */
public class VersionUpgradeAtomicCoherenceTest {

    private static final int EXPECTED_JAVA = 26;
    private static final int EXPECTED_CLASS_FILE_MAJOR = 70;  // 44 + 26
    private static final String EXPECTED_GRADLE = "9.5.1";
    private static final String EXPECTED_ARCHUNIT = "1.4.2";

    @Test
    void allVersionTouchPoints_shouldAgree_onJavaVersion() throws IOException {
        Path root = findProjectRoot();

        // 1. build.gradle.kts → VERSION_26
        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        Pattern srcPat = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Matcher srcMatcher = srcPat.matcher(buildGradle);
        assertThat(srcMatcher.find()).isTrue();
        int buildJava = Integer.parseInt(srcMatcher.group(1));

        // 2. README → "Java 26"
        String readme = Files.readString(root.resolve("README.md"));
        Pattern readmePat = Pattern.compile("Java\\s+(\\d+)");
        Matcher readmeMatcher = readmePat.matcher(readme);
        assertThat(readmeMatcher.find()).isTrue();
        int readmeJava = Integer.parseInt(readmeMatcher.group(1));

        // 3. JVM runtime
        int runtimeJava = Runtime.version().feature();

        // 4. Compiled bytecode
        int bytecodeJava = readBytecodeMajorVersion() - 44;

        // All should agree
        assertThat(buildJava).as("build.gradle.kts Java").isEqualTo(EXPECTED_JAVA);
        assertThat(readmeJava).as("README Java").isEqualTo(EXPECTED_JAVA);
        assertThat(runtimeJava).as("JVM runtime Java").isEqualTo(EXPECTED_JAVA);
        assertThat(bytecodeJava).as("Bytecode-derived Java").isEqualTo(EXPECTED_JAVA);
    }

    @Test
    void gradleVersion_shouldBeCompatibleWith_declaredJavaVersion() throws IOException {
        Path root = findProjectRoot();

        // Parse Java version from build.gradle.kts
        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        Pattern javaPat = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Matcher javaMatcher = javaPat.matcher(buildGradle);
        assertThat(javaMatcher.find()).isTrue();
        int javaVersion = Integer.parseInt(javaMatcher.group(1));

        // Parse Gradle version from wrapper
        Properties wrapperProps = new Properties();
        wrapperProps.load(Files.newInputStream(root.resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = wrapperProps.getProperty("distributionUrl");
        Pattern gradlePat = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher gradleMatcher = gradlePat.matcher(distUrl);
        assertThat(gradleMatcher.find()).isTrue();
        int gradleMajor = Integer.parseInt(gradleMatcher.group(1));
        int gradleMinor = Integer.parseInt(gradleMatcher.group(2));

        // Java 26 requires Gradle >= 9.4
        if (javaVersion == 26) {
            assertThat(gradleMajor).as("Gradle major for Java 26").isGreaterThanOrEqualTo(9);
            if (gradleMajor == 9) {
                assertThat(gradleMinor).as("Gradle minor for Java 26").isGreaterThanOrEqualTo(4);
            }
        }
    }

    @Test
    void archUnitVersion_shouldSupportDeclared_classFileMajorVersion() throws IOException {
        // Parse ArchUnit version from catalog
        String catalog = Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
        Pattern auPat = Pattern.compile("archunit\\s*=\\s*\"(\\d+)\\.(\\d+)\\.(\\d+)\"");
        Matcher auMatcher = auPat.matcher(catalog);
        assertThat(auMatcher.find()).isTrue();
        int auMajor = Integer.parseInt(auMatcher.group(1));
        int auMinor = Integer.parseInt(auMatcher.group(2));
        int auPatch = Integer.parseInt(auMatcher.group(3));

        // Read actual class file major version from compiled bytecode
        int classMajor = readBytecodeMajorVersion();

        // ArchUnit 1.4.2+ supports class file major version 70 (Java 26)
        if (classMajor >= 70) {
            boolean supported = (auMajor > 1)
                    || (auMajor == 1 && auMinor > 4)
                    || (auMajor == 1 && auMinor == 4 && auPatch >= 2);
            assertThat(supported)
                    .as("ArchUnit %d.%d.%d should support class file major version %d",
                            auMajor, auMinor, auPatch, classMajor)
                    .isTrue();
        }
    }

    @Test
    void versionFingerprint_shouldNotContain_anyPreviousVersionArtifacts() throws IOException {
        Path root = findProjectRoot();

        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        String readme = Files.readString(root.resolve("README.md"));
        String catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));
        String wrapperProps = Files.readString(root.resolve("gradle/wrapper/gradle-wrapper.properties"));

        // No Java 25 references
        assertThat(buildGradle).doesNotContain("VERSION_25");
        assertThat(readme).doesNotContain("Java 25");

        // No old Gradle 9.2.1 references
        assertThat(wrapperProps).doesNotContain("gradle-9.2.1");

        // No old ArchUnit 1.4.1 references
        assertThat(catalog).doesNotContain("\"1.4.1\"");
    }

    @Test
    void readmeInstallSection_shouldAlign_withBuildJavaVersion() throws IOException {
        Path root = findProjectRoot();

        // Extract Java version from build
        String buildGradle = Files.readString(root.resolve("build.gradle.kts"));
        Pattern javaPat = Pattern.compile("VERSION_(\\d+)");
        Matcher javaMatcher = javaPat.matcher(buildGradle);
        assertThat(javaMatcher.find()).isTrue();
        String buildJava = javaMatcher.group(1);

        // README install section should say "Install Java <same version>"
        String readme = Files.readString(root.resolve("README.md"));
        assertThat(readme)
                .as("README install section should match build Java version %s", buildJava)
                .containsPattern("(?i)install\\s+java\\s+" + buildJava);
    }

    @Test
    void wrapperProperties_newFields_shouldBe_presentAfterUpgrade() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));

        // These properties were added during the 9.2.1 → 9.5.1 upgrade
        assertThat(props.containsKey("retries"))
                .as("'retries' should be present (new in Gradle 9.5.1 wrapper)")
                .isTrue();
        assertThat(props.containsKey("retryBackOffMs"))
                .as("'retryBackOffMs' should be present (new in Gradle 9.5.1 wrapper)")
                .isTrue();

        // These should have existed before and still exist
        assertThat(props.containsKey("validateDistributionUrl"))
                .as("'validateDistributionUrl' should still be present after upgrade")
                .isTrue();
        assertThat(props.containsKey("networkTimeout"))
                .as("'networkTimeout' should still be present after upgrade")
                .isTrue();
    }

    private int readBytecodeMajorVersion() throws IOException {
        String classResource = "/com/educational/platform/java/VersionUpgradeAtomicCoherenceTest.class";
        try (InputStream is = getClass().getResourceAsStream(classResource)) {
            assertThat(is).as("Should load own class file").isNotNull();
            DataInputStream dis = new DataInputStream(is);
            dis.readInt();  // magic
            dis.readUnsignedShort();  // minor
            return dis.readUnsignedShort();  // major
        }
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
