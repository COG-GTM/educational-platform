package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Gradle–Java compatibility matrix constraints.
 * <p>
 * Per <a href="https://docs.gradle.org/current/userguide/compatibility.html">Gradle compatibility docs</a>,
 * each Java version requires a minimum Gradle version. Java 26 requires Gradle >= 9.4.0.
 * This test validates that the configured Gradle version meets or exceeds the minimum
 * for the project's configured Java target, and tests boundary conditions.
 */
public class GradleJavaCompatibilityMatrixTest {

    @ParameterizedTest(name = "Gradle {0}.{1}.{2} should be compatible with Java 26: {3}")
    @CsvSource({
            "9, 4, 0, true",   // minimum version for Java 26
            "9, 5, 0, true",   // above minimum
            "9, 5, 1, true",   // exact version used in project
            "10, 0, 0, true",  // future major
            "9, 3, 9, false",  // just below minimum minor
            "9, 2, 1, false",  // old project version (pre-upgrade)
            "8, 9, 0, false",  // previous major
    })
    void gradleVersion_java26Compatibility(int major, int minor, int patch, boolean expectedCompatible) {
        boolean compatible = isGradleCompatibleWithJava26(major, minor, patch);

        assertThat(compatible)
                .as("Gradle %d.%d.%d Java 26 compatibility", major, minor, patch)
                .isEqualTo(expectedCompatible);
    }

    @Test
    void projectGradleVersion_shouldBe_compatibleWithJava26() throws IOException {
        int[] version = parseGradleVersionFromWrapper();

        boolean compatible = isGradleCompatibleWithJava26(version[0], version[1], version[2]);

        assertThat(compatible)
                .as("Project Gradle version %d.%d.%d should be compatible with Java 26",
                        version[0], version[1], version[2])
                .isTrue();
    }

    @Test
    void projectGradleVersion_shouldExceed_minimumByAtLeastOnePatch() throws IOException {
        int[] version = parseGradleVersionFromWrapper();

        // 9.4.0 is the bare minimum; we expect at least 9.4.1 or higher for stability
        boolean exceedsMinimum = (version[0] > 9) ||
                (version[0] == 9 && version[1] > 4) ||
                (version[0] == 9 && version[1] == 4 && version[2] >= 1);

        assertThat(exceedsMinimum)
                .as("Gradle version %d.%d.%d should exceed bare minimum 9.4.0 for Java 26",
                        version[0], version[1], version[2])
                .isTrue();
    }

    @Test
    void projectJavaTarget_shouldBe_26() throws IOException {
        Path buildGradle = findProjectRoot().resolve("build.gradle.kts");
        String content = Files.readString(buildGradle);

        Pattern pattern = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();

        int javaTarget = Integer.parseInt(matcher.group(1));
        assertThat(javaTarget)
                .as("Java target version in build.gradle.kts")
                .isEqualTo(26);
    }

    @Test
    void gradleVersion_shouldNotBe_oldPreUpgradeVersion() throws IOException {
        int[] version = parseGradleVersionFromWrapper();

        boolean isOldVersion = version[0] == 9 && version[1] == 2 && version[2] == 1;

        assertThat(isOldVersion)
                .as("Gradle version should not be the old pre-upgrade 9.2.1")
                .isFalse();
    }

    @Test
    void gradleAndJava_versionPair_shouldBeDocumented() throws IOException {
        int[] gradleVersion = parseGradleVersionFromWrapper();
        Path buildGradle = findProjectRoot().resolve("build.gradle.kts");
        String content = Files.readString(buildGradle);

        Pattern pattern = Pattern.compile("sourceCompatibility\\s*=\\s*JavaVersion\\.VERSION_(\\d+)");
        Matcher matcher = pattern.matcher(content);
        assertThat(matcher.find()).isTrue();
        int javaTarget = Integer.parseInt(matcher.group(1));

        // The README should mention both versions
        Path readme = findProjectRoot().resolve("README.md");
        String readmeContent = Files.readString(readme);

        assertThat(readmeContent)
                .as("README should mention Java version %d", javaTarget)
                .contains("Java " + javaTarget);

        assertThat(readmeContent)
                .as("README should mention Gradle")
                .contains("Gradle");
    }

    private boolean isGradleCompatibleWithJava26(int major, int minor, int patch) {
        // Java 26 requires Gradle >= 9.4.0
        if (major > 9) return true;
        if (major < 9) return false;
        return minor >= 4;
    }

    private int[] parseGradleVersionFromWrapper() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));

        String distributionUrl = props.getProperty("distributionUrl");
        Pattern versionPattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = versionPattern.matcher(distributionUrl);
        assertThat(matcher.find()).as("Should parse Gradle version from distributionUrl").isTrue();

        return new int[]{
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        };
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
