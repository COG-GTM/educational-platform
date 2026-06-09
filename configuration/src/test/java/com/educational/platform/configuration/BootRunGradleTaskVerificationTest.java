package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Spring Boot plugin actually registers the expected
 * Gradle tasks (bootRun, bootJar, bootBuildImage) in the configuration
 * module. All existing tests validate build-file syntax or classpath
 * presence; this test runs the Gradle wrapper and inspects task output,
 * catching regressions where the plugin declaration is syntactically
 * correct but Gradle fails to resolve or apply it (e.g., version catalog
 * misconfiguration, settings.gradle.kts plugin resolution failure).
 */
class BootRunGradleTaskVerificationTest {

    private static Path projectRoot;
    private static List<String> taskOutput;

    @BeforeAll
    static void runGradleTasks() throws IOException, InterruptedException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;

        String wrapper = Files.exists(projectRoot.resolve("gradlew"))
                ? projectRoot.resolve("gradlew").toAbsolutePath().toString()
                : "gradle";

        ProcessBuilder pb = new ProcessBuilder(
                wrapper, ":configuration:tasks", "--all", "-q")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            taskOutput = reader.lines().collect(Collectors.toList());
        }
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        assertThat(finished)
                .as("Gradle tasks command must complete within 120 seconds")
                .isTrue();
        assertThat(process.exitValue())
                .as("Gradle tasks command must exit successfully (exit code 0)")
                .isEqualTo(0);
    }

    @Test
    void gradleTasks_shouldIncludeBootRunTask() {
        boolean hasBootRun = taskOutput.stream()
                .anyMatch(line -> line.contains("bootRun"));
        assertThat(hasBootRun)
                .as("bootRun task must be registered by the Spring Boot plugin — "
                        + "if missing, the plugin was not correctly applied via "
                        + "alias(libs.plugins.springboot) in configuration/build.gradle.kts")
                .isTrue();
    }

    @Test
    void gradleTasks_shouldIncludeBootJarTask() {
        boolean hasBootJar = taskOutput.stream()
                .anyMatch(line -> line.contains("bootJar"));
        assertThat(hasBootJar)
                .as("bootJar task must be registered by the Spring Boot plugin — "
                        + "bootJar produces the executable fat JAR with the Spring Boot loader")
                .isTrue();
    }

    @Test
    void gradleTasks_shouldIncludeBootBuildImageTask() {
        boolean hasBootBuildImage = taskOutput.stream()
                .anyMatch(line -> line.contains("bootBuildImage"));
        assertThat(hasBootBuildImage)
                .as("bootBuildImage task must be registered by the Spring Boot plugin — "
                        + "it creates an OCI container image via Cloud Native Buildpacks")
                .isTrue();
    }
}
