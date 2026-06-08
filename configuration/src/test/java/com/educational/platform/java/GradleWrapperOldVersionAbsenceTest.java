package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard ensuring no references to the old Gradle version (9.2.1)
 * remain in any wrapper-related file after the upgrade to 9.5.1.
 * <p>
 * {@link VersionUpgradeRegressionGuardTest} checks that
 * {@code gradle-wrapper.properties} does not reference 9.2.1.
 * This test extends that guard to the wrapper <em>scripts</em>
 * ({@code gradlew}, {@code gradlew.bat}) and validates that neither
 * the properties file nor the scripts contain any trace of the old version.
 * <p>
 * The wrapper scripts were regenerated during the Gradle 9.2.1 → 9.5.1
 * upgrade. If only the properties file was updated but the scripts were
 * not regenerated, the scripts could embed stale version references or
 * incompatible bootstrap logic.
 */
public class GradleWrapperOldVersionAbsenceTest {

    private static final String OLD_GRADLE_VERSION = "9.2.1";
    private static final String CURRENT_GRADLE_VERSION = "9.5.1";

    @Test
    void gradleWrapperProperties_shouldNotReference_oldVersion() throws IOException {
        String content = Files.readString(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties"));

        assertThat(content)
                .as("gradle-wrapper.properties should not reference old Gradle %s", OLD_GRADLE_VERSION)
                .doesNotContain("gradle-" + OLD_GRADLE_VERSION);
    }

    @Test
    void gradleWrapperProperties_shouldReference_currentVersion() throws IOException {
        String content = Files.readString(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties"));

        assertThat(content)
                .as("gradle-wrapper.properties should reference current Gradle %s", CURRENT_GRADLE_VERSION)
                .contains("gradle-" + CURRENT_GRADLE_VERSION);
    }

    @Test
    void gradlewScript_shouldNotContain_oldVersionString() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        if (Files.exists(gradlew)) {
            String content = Files.readString(gradlew);
            assertThat(content)
                    .as("gradlew script should not contain old Gradle version %s", OLD_GRADLE_VERSION)
                    .doesNotContain(OLD_GRADLE_VERSION);
        }
    }

    @Test
    void gradlewBatScript_shouldNotContain_oldVersionString() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        if (Files.exists(gradlewBat)) {
            String content = Files.readString(gradlewBat);
            assertThat(content)
                    .as("gradlew.bat script should not contain old Gradle version %s", OLD_GRADLE_VERSION)
                    .doesNotContain(OLD_GRADLE_VERSION);
        }
    }

    @ParameterizedTest(name = "No wrapper file should reference Gradle {0}")
    @ValueSource(strings = {"9.0.0", "9.1.0", "9.2.0", "9.2.1", "9.3.0", "9.3.1"})
    void noWrapperFile_shouldReference_priorGradleVersion(String oldVersion) throws IOException {
        Path root = findProjectRoot();
        String propsContent = Files.readString(root.resolve("gradle/wrapper/gradle-wrapper.properties"));

        assertThat(propsContent)
                .as("gradle-wrapper.properties should not reference Gradle %s", oldVersion)
                .doesNotContain("gradle-" + oldVersion);
    }

    @Test
    void distributionUrl_shouldReference_onlyCurrentVersion() throws IOException {
        String content = Files.readString(
                findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties"));

        // Extract the version from distributionUrl
        java.util.regex.Pattern versionPattern =
                java.util.regex.Pattern.compile("gradle-(\\d+\\.\\d+\\.\\d+)");
        java.util.regex.Matcher matcher = versionPattern.matcher(content);

        assertThat(matcher.find())
                .as("distributionUrl should contain a parseable Gradle version")
                .isTrue();

        String extractedVersion = matcher.group(1);
        assertThat(extractedVersion)
                .as("distributionUrl version should be exactly %s", CURRENT_GRADLE_VERSION)
                .isEqualTo(CURRENT_GRADLE_VERSION);

        // Ensure no second version reference (e.g., stale comment)
        assertThat(matcher.find())
                .as("distributionUrl should contain exactly one Gradle version reference")
                .isFalse();
    }

    @Test
    void wrapperJar_shouldExist_afterUpgrade() {
        Path wrapperJar = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        assertThat(wrapperJar)
                .as("gradle-wrapper.jar should exist after Gradle %s upgrade", CURRENT_GRADLE_VERSION)
                .exists()
                .isRegularFile();

        assertThat(wrapperJar.toFile().length())
                .as("gradle-wrapper.jar should have a reasonable file size")
                .isGreaterThan(10_000);
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
