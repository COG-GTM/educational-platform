package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the format and structural integrity of the Gradle wrapper scripts
 * after the 9.2.1 → 9.5.1 upgrade.
 * <p>
 * {@link WrapperScriptCrossPlatformConsistencyTest} validates cross-platform
 * consistency (both scripts use -jar, both reference the same JAR).
 * {@link GradleWrapperScriptPermissionsTest} validates file permissions.
 * <p>
 * This test focuses on <em>format correctness</em>:
 * <ul>
 *   <li>Unix script shebang line</li>
 *   <li>Windows batch script CRLF line endings</li>
 *   <li>No UTF-8 BOM in either script</li>
 *   <li>Unix script uses POSIX-compliant variable references</li>
 *   <li>Windows script uses proper batch comment style</li>
 *   <li>Scripts reference APP_HOME for portability</li>
 * </ul>
 */
public class GradleWrapperScriptFormatValidationTest {

    // --- Unix script format ---

    @Test
    void gradlew_shouldStartWith_shebangLine() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should start with a shebang line (#!/...)")
                .startsWith("#!/");
    }

    @Test
    void gradlew_shebang_shouldUsePosixShell() throws IOException {
        String firstLine = readGradlew().lines().findFirst().orElse("");

        assertThat(firstLine)
                .as("gradlew shebang should use a POSIX shell (e.g., #!/bin/sh or #!/usr/bin/env sh)")
                .matches("#!/(usr/)?bin/(ba)?sh|#!/usr/bin/env\\s+(ba)?sh");
    }

    @Test
    void gradlew_shouldUse_appHomeVariable() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should use APP_HOME variable for portability")
                .contains("APP_HOME");
    }

    @Test
    void gradlew_shouldReference_javaHome() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should reference JAVA_HOME for JDK resolution")
                .contains("JAVA_HOME");
    }

    @Test
    void gradlew_shouldNotContain_hardcodedPaths() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should not contain hardcoded /usr/lib/jvm paths")
                .doesNotContain("/usr/lib/jvm/")
                .doesNotContain("/opt/java/");
    }

    @Test
    void gradlew_shouldNotContain_utf8Bom() throws IOException {
        byte[] bytes = Files.readAllBytes(findProjectRoot().resolve("gradlew"));

        if (bytes.length >= 3) {
            boolean hasBom = (bytes[0] == (byte) 0xEF
                    && bytes[1] == (byte) 0xBB
                    && bytes[2] == (byte) 0xBF);
            assertThat(hasBom)
                    .as("gradlew should not have a UTF-8 BOM (causes parsing issues)")
                    .isFalse();
        }
    }

    // --- Windows batch script format ---

    @Test
    void gradlewBat_shouldUse_windowsLineEndings() throws IOException {
        byte[] bytes = Files.readAllBytes(findProjectRoot().resolve("gradlew.bat"));
        String content = new String(bytes, StandardCharsets.UTF_8);

        long crlfCount = 0;
        long lfOnlyCount = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                if (i > 0 && content.charAt(i - 1) == '\r') {
                    crlfCount++;
                } else {
                    lfOnlyCount++;
                }
            }
        }

        assertThat(crlfCount)
                .as("gradlew.bat should have CRLF line endings (Windows convention)")
                .isGreaterThan(0);

        assertThat(lfOnlyCount)
                .as("gradlew.bat should not mix LF-only with CRLF line endings")
                .isZero();
    }

    @Test
    void gradlewBat_shouldStartWith_atSign() throws IOException {
        byte[] bytes = Files.readAllBytes(findProjectRoot().resolve("gradlew.bat"));
        String content = new String(bytes, StandardCharsets.UTF_8);
        // Skip BOM if present
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        String firstNonEmptyLine = content.lines()
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse("");

        assertThat(firstNonEmptyLine)
                .as("gradlew.bat first executable line should start with @")
                .startsWith("@");
    }

    @Test
    void gradlewBat_shouldUse_remComments() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should use @rem or rem for comments (batch style)")
                .containsPattern("(?i)@?rem\\s");
    }

    @Test
    void gradlewBat_shouldReference_javaHome() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should reference JAVA_HOME")
                .contains("JAVA_HOME");
    }

    @Test
    void gradlewBat_shouldNotContain_utf8Bom() throws IOException {
        byte[] bytes = Files.readAllBytes(findProjectRoot().resolve("gradlew.bat"));

        if (bytes.length >= 3) {
            boolean hasBom = (bytes[0] == (byte) 0xEF
                    && bytes[1] == (byte) 0xBB
                    && bytes[2] == (byte) 0xBF);
            assertThat(hasBom)
                    .as("gradlew.bat should not have a UTF-8 BOM (can break Windows cmd.exe)")
                    .isFalse();
        }
    }

    @Test
    void gradlewBat_shouldUse_appBaseVariable() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should use APP_HOME or similar variable for portability")
                .containsPattern("(?i)(APP_HOME|APP_BASE_NAME)");
    }

    // --- Helpers ---

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
