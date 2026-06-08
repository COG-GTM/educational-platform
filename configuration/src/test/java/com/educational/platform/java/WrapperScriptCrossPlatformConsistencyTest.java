package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates cross-platform consistency between {@code gradlew} (Unix) and
 * {@code gradlew.bat} (Windows) wrapper scripts after the Gradle 9.2.1 → 9.5.1
 * upgrade.
 * <p>
 * Both scripts must reference the same wrapper JAR path, use the same invocation
 * strategy ({@code -jar}), and carry matching license headers. If one script is
 * updated but the other is not, builds will behave differently across platforms.
 * <p>
 * Individual script tests exist in {@link GradleWrapperScriptTest}. This test
 * focuses on the <em>consistency</em> between the two scripts.
 */
public class WrapperScriptCrossPlatformConsistencyTest {

    @Test
    void bothScripts_shouldExist() {
        Path root = findProjectRoot();

        assertThat(root.resolve("gradlew"))
                .as("Unix wrapper script should exist")
                .exists();

        assertThat(root.resolve("gradlew.bat"))
                .as("Windows wrapper script should exist")
                .exists();
    }

    @Test
    void bothScripts_shouldUse_jarInvocation() throws IOException {
        String gradlew = readGradlew();
        String gradlewBat = readGradlewBat();

        assertThat(gradlew)
                .as("gradlew should use -jar invocation")
                .contains("-jar");
        assertThat(gradlewBat)
                .as("gradlew.bat should use -jar invocation")
                .contains("-jar");
    }

    @Test
    void bothScripts_shouldReference_sameWrapperJarRelativePath() throws IOException {
        String gradlew = readGradlew();
        String gradlewBat = readGradlewBat();

        assertThat(gradlew)
                .as("gradlew should reference wrapper JAR under gradle/wrapper/")
                .contains("gradle/wrapper/gradle-wrapper.jar");

        assertThat(gradlewBat)
                .as("gradlew.bat should reference wrapper JAR under gradle\\wrapper\\")
                .contains("gradle\\wrapper\\gradle-wrapper.jar");
    }

    @Test
    void bothScripts_shouldNotUse_legacyClasspathInvocation() throws IOException {
        String gradlew = readGradlew();
        String gradlewBat = readGradlewBat();

        assertThat(gradlew)
                .as("gradlew should not use GradleWrapperMain")
                .doesNotContain("org.gradle.wrapper.GradleWrapperMain");
        assertThat(gradlewBat)
                .as("gradlew.bat should not use GradleWrapperMain")
                .doesNotContain("org.gradle.wrapper.GradleWrapperMain");
    }

    @Test
    void bothScripts_shouldHave_spdxLicenseIdentifier() throws IOException {
        String gradlew = readGradlew();
        String gradlewBat = readGradlewBat();

        assertThat(gradlew)
                .as("gradlew should have SPDX license")
                .contains("SPDX-License-Identifier: Apache-2.0");
        assertThat(gradlewBat)
                .as("gradlew.bat should have SPDX license")
                .contains("SPDX-License-Identifier: Apache-2.0");
    }

    @Test
    void bothScripts_shouldBeNonEmpty() throws IOException {
        Path root = findProjectRoot();

        assertThat(Files.size(root.resolve("gradlew")))
                .as("gradlew should not be empty")
                .isGreaterThan(100);

        assertThat(Files.size(root.resolve("gradlew.bat")))
                .as("gradlew.bat should not be empty")
                .isGreaterThan(100);
    }

    @Test
    void wrapperJar_referencedByBothScripts_shouldExist() {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        assertThat(jarPath)
                .as("gradle-wrapper.jar referenced by both scripts should exist")
                .exists()
                .isRegularFile();
    }

    @Test
    void unixScript_shouldHave_unixLineEndings() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        byte[] bytes = Files.readAllBytes(gradlew);
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

        long crlfCount = content.chars()
                .filter(c -> c == '\r')
                .count();

        assertThat(crlfCount)
                .as("gradlew should use Unix line endings (LF), not Windows (CRLF)")
                .isZero();
    }

    private String readGradlew() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradlew"));
    }

    private String readGradlewBat() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradlew.bat"));
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
