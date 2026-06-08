package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper scripts use the modern patterns introduced
 * in the Gradle 9.2.1 → 9.5.1 upgrade.
 * <p>
 * {@link GradleWrapperScriptTest} validates the high-level invocation mode
 * ({@code -jar} vs CLASSPATH) and label structure. This test validates the
 * lower-level shell patterns that changed between versions:
 * <ul>
 *   <li>{@code gradlew}: uses {@code cd -P} for physical directory resolution
 *       (avoids symlink confusion) and {@code printf '%s\n' "$PWD"} instead of
 *       {@code pwd -P} (more portable across POSIX shells)</li>
 *   <li>{@code gradlew.bat}: uses {@code endlocal &} inline pattern to clear
 *       local variables before executing Java, preventing variable leakage</li>
 * </ul>
 * These patterns are security- and correctness-relevant: symlink-following
 * {@code pwd -P} was replaced because some shells treat it inconsistently,
 * and the {@code endlocal &} pattern prevents stale environment pollution.
 */
public class GradleWrapperScriptModernPatternsTest {

    // --- gradlew: APP_HOME resolution ---

    @Test
    void gradlew_shouldUse_cdDashP_forPhysicalDirectoryResolution() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should use 'cd -P' to resolve physical directory (avoids symlink confusion)")
                .contains("cd -P");
    }

    @Test
    void gradlew_shouldUse_printf_forPwdOutput() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should use printf to output PWD (more portable than pwd -P)")
                .contains("printf '%s\\n' \"$PWD\"");
    }

    @Test
    void gradlew_shouldNotUse_oldPwdDashP_forAppHome() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should not use the old 'pwd -P' pattern for APP_HOME resolution")
                .doesNotContain("pwd -P");
    }

    @Test
    void gradlew_appHomeResolution_shouldBe_singleLine() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should have the complete APP_HOME resolution pattern")
                .containsPattern("APP_HOME=\\$\\( cd -P .* && printf '%s\\\\n' \"\\$PWD\" \\)");
    }

    // --- gradlew: comment accuracy ---

    @Test
    void gradlew_shouldNotContain_duplicateJavaOptsComment() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should not reference JAVA_OPTS twice in the same comment line")
                .doesNotContain("JAVA_OPTS, JAVA_OPTS");
    }

    // --- gradlew.bat: endlocal & inline execution ---

    @Test
    void gradlewBat_shouldUse_endlocalInline_forJavaExecution() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should use 'endlocal &' to clear locals before java execution")
                .contains("endlocal & \"%JAVA_EXE%\"");
    }

    @Test
    void gradlewBat_shouldNotUse_oldEndlocalPattern() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should not use the old 'if \"%OS%\"==\"Windows_NT\" endlocal' pattern")
                .doesNotContain("if \"%OS%\"==\"Windows_NT\" endlocal");
    }

    @Test
    void gradlewBat_shouldCall_exitWithErrorLevel_afterExecution() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should call :exitWithErrorLevel after java execution")
                .contains("call :exitWithErrorLevel");
    }

    @Test
    void gradlewBat_shouldNotUse_oldExitCodeVariable() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should not use old EXIT_CODE variable pattern")
                .doesNotContain("set EXIT_CODE=");

        assertThat(content)
                .as("gradlew.bat should not reference GRADLE_EXIT_CONSOLE")
                .doesNotContain("GRADLE_EXIT_CONSOLE");
    }

    // --- gradlew.bat: COMSPEC error exit pattern ---

    @Test
    void gradlewBat_errorExit_shouldUse_comspecWithExitCode1() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should use COMSPEC /c exit 1 for error exits (not goto fail)")
                .contains("\"%COMSPEC%\" /c exit 1");
    }

    @Test
    void gradlewBat_exitWithErrorLevel_shouldUse_comspecWithErrorLevel() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat :exitWithErrorLevel should use COMSPEC with ERRORLEVEL")
                .contains("\"%COMSPEC%\" /c exit %ERRORLEVEL%");
    }

    // --- helpers ---

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
