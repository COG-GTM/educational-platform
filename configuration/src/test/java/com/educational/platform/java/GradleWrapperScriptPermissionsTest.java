package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Gradle wrapper script file properties after the 9.2.1 → 9.5.1 upgrade.
 * <p>
 * On Unix systems, {@code gradlew} must be executable. If the execute permission is
 * missing (e.g., after a bad Git clone or file copy), the script cannot be invoked
 * directly and users get a "Permission denied" error. This test also validates
 * basic structural properties of both wrapper scripts.
 */
public class GradleWrapperScriptPermissionsTest {

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void gradlew_shouldBeExecutable_onUnix() {
        Path gradlew = findProjectRoot().resolve("gradlew");

        assertThat(Files.isExecutable(gradlew))
                .as("gradlew should have execute permission on Unix")
                .isTrue();
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void gradlew_shouldBeReadable_onUnix() {
        Path gradlew = findProjectRoot().resolve("gradlew");

        assertThat(Files.isReadable(gradlew))
                .as("gradlew should be readable")
                .isTrue();
    }

    @Test
    void gradlew_shouldNotBeEmpty() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");

        assertThat(Files.size(gradlew))
                .as("gradlew should not be empty")
                .isGreaterThan(100);
    }

    @Test
    void gradlewBat_shouldNotBeEmpty() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");

        assertThat(Files.size(gradlewBat))
                .as("gradlew.bat should not be empty")
                .isGreaterThan(100);
    }

    @Test
    void gradlew_shouldContain_gradleAppNameReference() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should reference Gradle app name")
                .containsPattern("(?i)gradle");
    }

    @Test
    void gradlewBat_shouldContain_gradleAppNameReference() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should reference Gradle app name")
                .containsIgnoringCase("gradle");
    }

    @Test
    void gradlew_shouldReferenceWrapperJar() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should reference gradle-wrapper.jar")
                .contains("gradle-wrapper.jar");
    }

    @Test
    void gradlewBat_shouldReferenceWrapperJar() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should reference gradle-wrapper.jar")
                .contains("gradle-wrapper.jar");
    }

    @Test
    void gradlew_shouldReference_javaExecution() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should invoke Java (exec or java command)")
                .containsPattern("(?i)(exec|java)");
    }

    @Test
    void gradlewBat_shouldReference_javaExecution() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should invoke Java")
                .containsIgnoringCase("java");
    }

    @Test
    void gradlew_fileSizeShouldBe_reasonable() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        long size = Files.size(gradlew);

        // Typical gradlew is 5-15KB
        assertThat(size)
                .as("gradlew size should be reasonable (not truncated or bloated)")
                .isBetween(1_000L, 50_000L);
    }

    @Test
    void gradlewBat_fileSizeShouldBe_reasonable() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        long size = Files.size(gradlewBat);

        // Typical gradlew.bat is 2-10KB
        assertThat(size)
                .as("gradlew.bat size should be reasonable (not truncated or bloated)")
                .isBetween(500L, 30_000L);
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
