package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coherence test validating that all version-related artifacts
 * in the project are mutually consistent after the Java 26 upgrade.
 * <p>
 * Unlike the individual tests which check one file or one relationship,
 * this test cross-references multiple sources simultaneously:
 * <ul>
 *   <li>README documentation ↔ build.gradle.kts ↔ wrapper properties ↔ version catalog</li>
 *   <li>Java target ↔ required Gradle minimum ↔ actual Gradle version</li>
 *   <li>ArchUnit version ↔ class file major version it must support</li>
 *   <li>Runtime JVM version ↔ build target version ↔ bytecode version</li>
 * </ul>
 */
public class UpgradeCoherenceIntegrationTest {

    @Test
    void fullStack_javaVersion_shouldBeCoherent_acrossAllArtifacts() throws IOException {
        Path root = findProjectRoot();

        // 1. Extract Java version from build.gradle.kts
        String buildContent = Files.readString(root.resolve("build.gradle.kts"));
        Matcher buildMatcher = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(buildMatcher.find()).isTrue();
        int buildJavaVersion = Integer.parseInt(buildMatcher.group(1));

        // 2. Extract from README
        String readme = Files.readString(root.resolve("README.md"));
        assertThat(readme).contains("Java " + buildJavaVersion);

        // 3. Verify runtime matches
        int runtimeVersion = Runtime.version().feature();
        assertThat(runtimeVersion)
                .as("Runtime JVM version should match build target")
                .isEqualTo(buildJavaVersion);

        // 4. Verify class file version formula
        int expectedMajor = 44 + buildJavaVersion;
        String classVersion = System.getProperty("java.class.version");
        int actualMajor = (int) Double.parseDouble(classVersion);
        assertThat(actualMajor)
                .as("Class file major version should be 44 + %d = %d", buildJavaVersion, expectedMajor)
                .isEqualTo(expectedMajor);
    }

    @Test
    void fullStack_gradleVersion_shouldMeetJava26Minimum() throws IOException {
        Path root = findProjectRoot();

        // Extract Java version from build
        String buildContent = Files.readString(root.resolve("build.gradle.kts"));
        Matcher javaMatcher = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(javaMatcher.find()).isTrue();
        int javaVersion = Integer.parseInt(javaMatcher.group(1));

        // Extract Gradle version from wrapper
        Properties wrapperProps = new Properties();
        wrapperProps.load(Files.newInputStream(root.resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = wrapperProps.getProperty("distributionUrl");
        Matcher gradleMatcher = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)").matcher(distUrl);
        assertThat(gradleMatcher.find()).isTrue();

        int gradleMajor = Integer.parseInt(gradleMatcher.group(1));
        int gradleMinor = Integer.parseInt(gradleMatcher.group(2));

        // Java 26 requires Gradle >= 9.4.0
        if (javaVersion >= 26) {
            assertThat(gradleMajor).isGreaterThanOrEqualTo(9);
            if (gradleMajor == 9) {
                assertThat(gradleMinor)
                        .as("Gradle 9.x must have minor >= 4 for Java %d", javaVersion)
                        .isGreaterThanOrEqualTo(4);
            }
        }
    }

    @Test
    void fullStack_archunitVersion_shouldSupportCurrentBytecode() throws IOException {
        Path root = findProjectRoot();

        // Extract ArchUnit version from catalog
        String catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));
        Matcher archMatcher = Pattern.compile("archunit\\s*=\\s*\"(\\d+)\\.(\\d+)\\.(\\d+)\"").matcher(catalog);
        assertThat(archMatcher.find()).isTrue();
        int archMajor = Integer.parseInt(archMatcher.group(1));
        int archMinor = Integer.parseInt(archMatcher.group(2));
        int archPatch = Integer.parseInt(archMatcher.group(3));

        // Extract Java version to determine required class file major version
        String buildContent = Files.readString(root.resolve("build.gradle.kts"));
        Matcher javaMatcher = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(javaMatcher.find()).isTrue();
        int javaVersion = Integer.parseInt(javaMatcher.group(1));
        int classFileMajor = 44 + javaVersion;

        // ArchUnit 1.4.2+ supports class file major version 70 (Java 26)
        if (classFileMajor >= 70) {
            boolean supported = (archMajor > 1)
                    || (archMajor == 1 && archMinor > 4)
                    || (archMajor == 1 && archMinor == 4 && archPatch >= 2);
            assertThat(supported)
                    .as("ArchUnit %d.%d.%d must support class file major %d (Java %d)",
                            archMajor, archMinor, archPatch, classFileMajor, javaVersion)
                    .isTrue();
        }
    }

    @Test
    void noStaleVersionReferences_shouldExist_inAnyBuildFile() throws IOException {
        Path root = findProjectRoot();

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().endsWith(".gradle.kts"))
                    .filter(p -> !p.toString().contains(".gradle/"))
                    .toList();

            for (Path buildFile : buildFiles) {
                String content = Files.readString(buildFile);
                assertThat(content)
                        .as("Build file %s should not reference old Java 25", buildFile.getFileName())
                        .doesNotContain("VERSION_25");
            }
        }
    }

    @ParameterizedTest(name = "Version pair coherence: Java {0} requires Gradle >= {1} and ArchUnit >= {2}")
    @CsvSource({
            "26, '9.4.0', '1.4.2'"
    })
    void versionTriple_shouldBeCoherent(int javaVersion, String minGradle, String minArchUnit) throws IOException {
        Path root = findProjectRoot();

        // Verify actual Gradle version >= minimum
        Properties wrapperProps = new Properties();
        wrapperProps.load(Files.newInputStream(root.resolve("gradle/wrapper/gradle-wrapper.properties")));
        String distUrl = wrapperProps.getProperty("distributionUrl");
        Matcher gradleMatcher = Pattern.compile("gradle-(\\d+\\.\\d+\\.\\d+)").matcher(distUrl);
        assertThat(gradleMatcher.find()).isTrue();
        String actualGradle = gradleMatcher.group(1);

        assertThat(compareVersionStrings(actualGradle, minGradle))
                .as("Actual Gradle %s should be >= minimum %s for Java %d",
                        actualGradle, minGradle, javaVersion)
                .isGreaterThanOrEqualTo(0);

        // Verify actual ArchUnit version >= minimum
        String catalog = Files.readString(root.resolve("gradle/libs.versions.toml"));
        Matcher archMatcher = Pattern.compile("archunit\\s*=\\s*\"(\\d+\\.\\d+\\.\\d+)\"").matcher(catalog);
        assertThat(archMatcher.find()).isTrue();
        String actualArchUnit = archMatcher.group(1);

        assertThat(compareVersionStrings(actualArchUnit, minArchUnit))
                .as("Actual ArchUnit %s should be >= minimum %s for Java %d",
                        actualArchUnit, minArchUnit, javaVersion)
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void readmeInstallInstructions_shouldMatch_buildTarget() throws IOException {
        Path root = findProjectRoot();

        // Get build target version
        String buildContent = Files.readString(root.resolve("build.gradle.kts"));
        Matcher javaMatcher = Pattern.compile("VERSION_(\\d+)").matcher(buildContent);
        assertThat(javaMatcher.find()).isTrue();
        String targetVersion = javaMatcher.group(1);

        // README install section should mention same version
        String readme = Files.readString(root.resolve("README.md"));
        assertThat(readme)
                .as("README install instructions should reference Java %s", targetVersion)
                .containsPattern("(?i)install\\s+java\\s+" + targetVersion);
    }

    private int compareVersionStrings(String v1, String v2) {
        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher m1 = semver.matcher(v1);
        Matcher m2 = semver.matcher(v2);
        assertThat(m1.find() && m2.find()).isTrue();

        int cmp = Integer.compare(Integer.parseInt(m1.group(1)), Integer.parseInt(m2.group(1)));
        if (cmp != 0) return cmp;
        cmp = Integer.compare(Integer.parseInt(m1.group(2)), Integer.parseInt(m2.group(2)));
        if (cmp != 0) return cmp;
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
