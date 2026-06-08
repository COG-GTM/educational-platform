package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the encoding and line-ending conventions of Gradle wrapper scripts
 * shipped with the Gradle 9.5.1 upgrade.
 * <p>
 * {@code gradlew} (Unix) must use LF line endings for Bash compatibility.
 * {@code gradlew.bat} (Windows) must use CRLF line endings for cmd.exe compatibility.
 * Incorrect line endings cause "bad interpreter" errors on Unix or silent failures
 * on Windows.
 */
public class GradleWrapperScriptEncodingTest {

    @Test
    void gradlew_shouldUse_unixLineEndings() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        byte[] bytes = Files.readAllBytes(gradlew);

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\n' && i > 0 && bytes[i - 1] == '\r') {
                throw new AssertionError(
                        "gradlew should use Unix line endings (LF), but found CRLF at byte offset " + (i - 1));
            }
        }
    }

    @Test
    void gradlewBat_shouldUse_windowsLineEndings() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        byte[] bytes = Files.readAllBytes(gradlewBat);

        boolean foundCrlf = false;
        for (int i = 0; i < bytes.length - 1; i++) {
            if (bytes[i] == '\r' && bytes[i + 1] == '\n') {
                foundCrlf = true;
                break;
            }
        }

        assertThat(foundCrlf)
                .as("gradlew.bat should use Windows line endings (CRLF)")
                .isTrue();
    }

    @Test
    void gradlew_shouldStartWith_shebangLine() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        byte[] bytes = Files.readAllBytes(gradlew);

        assertThat(bytes.length).isGreaterThan(2);
        assertThat(bytes[0]).as("First byte should be '#'").isEqualTo((byte) '#');
        assertThat(bytes[1]).as("Second byte should be '!'").isEqualTo((byte) '!');
    }

    @Test
    void gradlew_shouldBeEncodedIn_validUtf8() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        byte[] bytes = Files.readAllBytes(gradlew);

        // The file may contain valid UTF-8 multibyte characters (e.g. © in the
        // copyright header), so verify the entire file decodes as valid UTF-8.
        String decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        byte[] reEncoded = decoded.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(reEncoded)
                .as("gradlew should round-trip through UTF-8 without data loss")
                .isEqualTo(bytes);

        // No NUL bytes
        for (int i = 0; i < bytes.length; i++) {
            assertThat(bytes[i])
                    .as("gradlew should not contain NUL bytes, found at offset %d", i)
                    .isNotEqualTo((byte) 0x00);
        }
    }

    @Test
    void gradlewBat_shouldNotStartWith_bom() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        byte[] bytes = Files.readAllBytes(gradlewBat);

        assertThat(bytes.length).isGreaterThan(3);
        boolean hasBom = (bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);

        assertThat(hasBom)
                .as("gradlew.bat should not have a UTF-8 BOM (causes issues with cmd.exe)")
                .isFalse();
    }

    @Test
    void gradlew_shouldNotContain_windowsCarriageReturns() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        byte[] bytes = Files.readAllBytes(gradlew);

        for (byte b : bytes) {
            assertThat(b)
                    .as("gradlew should not contain carriage return (0x0D)")
                    .isNotEqualTo((byte) '\r');
        }
    }

    @Test
    void gradlewBat_shouldStartWith_atRemComment() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should start with @rem comment block")
                .startsWith("@rem");
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
