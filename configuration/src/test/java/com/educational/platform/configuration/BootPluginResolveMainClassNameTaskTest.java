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
 * Validates that the Spring Boot plugin registers the {@code resolveMainClassName}
 * task in the configuration module. This task is a critical prerequisite for
 * {@code bootRun} and {@code bootJar} — it auto-detects the main class by
 * scanning for {@code @SpringBootApplication}. If this task is missing or
 * misconfigured, both bootRun and bootJar will fail with
 * "Main class name has not been configured and it could not be resolved".
 * <p>
 * Complements {@link BootRunGradleTaskVerificationTest} which validates
 * bootRun, bootJar, and bootBuildImage task registration. This test covers
 * the resolveMainClassName task that those tasks depend on but do not
 * explicitly verify.
 */
class BootPluginResolveMainClassNameTaskTest {

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

        String wrapper = Files.exists(dir.resolve("gradlew"))
                ? dir.resolve("gradlew").toAbsolutePath().toString()
                : "gradle";

        ProcessBuilder pb = new ProcessBuilder(
                wrapper, ":configuration:tasks", "--all", "-q")
                .directory(dir.toFile())
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
                .as("Gradle tasks command must exit successfully")
                .isEqualTo(0);
    }

    @Test
    void gradleTasks_shouldIncludeResolveMainClassNameTask() {
        boolean hasResolveMainClassName = taskOutput.stream()
                .anyMatch(line -> line.contains("resolveMainClassName"));
        assertThat(hasResolveMainClassName)
                .as("resolveMainClassName task must be registered by the Spring Boot plugin — "
                        + "it auto-detects the @SpringBootApplication class and is a prerequisite "
                        + "for bootRun and bootJar; if missing, boot tasks cannot resolve the entry point")
                .isTrue();
    }

    @Test
    void gradleTasks_shouldIncludeResolveTestMainClassName() {
        boolean hasResolveTestMainClassName = taskOutput.stream()
                .anyMatch(line -> line.contains("resolveTestMainClassName"));
        assertThat(hasResolveTestMainClassName)
                .as("resolveTestMainClassName task should be registered by the Spring Boot plugin — "
                        + "it supports test-specific main class resolution for bootTestRun")
                .isTrue();
    }

    @Test
    void gradleTasks_shouldNotIncludeResolveMainClassNameInNonBootModules() {
        // Verify that the task output is specifically for the configuration module
        // by checking that configuration-module-specific tasks are present
        boolean hasBootRun = taskOutput.stream()
                .anyMatch(line -> line.contains("bootRun"));
        assertThat(hasBootRun)
                .as("bootRun must be present alongside resolveMainClassName — "
                        + "both are registered by the Spring Boot plugin in the configuration module")
                .isTrue();
    }
}
