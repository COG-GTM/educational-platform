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
 * Validates the Gradle task dependency graph for the configuration module after
 * the Spring Boot plugin is applied. The boot plugin modifies the default task
 * graph: {@code bootRun} depends on {@code classes} (compileJava + processResources),
 * and {@code bootJar} replaces the standard {@code jar} task as the primary
 * archive task. This test runs {@code ./gradlew :configuration:bootRun --dry-run}
 * to verify the complete task execution order without actually starting the app.
 * <p>
 * Complements {@link BootRunGradleTaskVerificationTest} (task existence via
 * tasks --all) and {@link BootPluginTaskRegistrationTest} (file-based validation).
 * This test validates the runtime task execution order and dependency chain.
 */
class ConfigurationModuleGradleTaskGraphTest {

    private static Path projectRoot;
    private static List<String> bootRunDryRunOutput;
    private static int exitCode;

    @BeforeAll
    static void runBootRunDryRun() throws IOException, InterruptedException {
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
                wrapper, ":configuration:bootRun", "--dry-run", "-q")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            bootRunDryRunOutput = reader.lines().collect(Collectors.toList());
        }
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        assertThat(finished)
                .as("Gradle bootRun --dry-run must complete within 120 seconds")
                .isTrue();
        exitCode = process.exitValue();
    }

    @Test
    void bootRunDryRun_shouldSucceed() {
        assertThat(exitCode)
                .as("bootRun --dry-run must succeed (exit code 0) — "
                        + "failure indicates the Spring Boot plugin cannot construct "
                        + "a valid task execution plan for bootRun")
                .isEqualTo(0);
    }

    @Test
    void bootRunDryRun_shouldIncludeCompileJava() {
        boolean hasCompileJava = bootRunDryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:compileJava"));
        assertThat(hasCompileJava)
                .as("bootRun task graph must include compileJava — "
                        + "the application source must be compiled before running")
                .isTrue();
    }

    @Test
    void bootRunDryRun_shouldIncludeProcessResources() {
        boolean hasProcessResources = bootRunDryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:processResources"));
        assertThat(hasProcessResources)
                .as("bootRun task graph must include processResources — "
                        + "application.properties must be available at runtime")
                .isTrue();
    }

    @Test
    void bootRunDryRun_compileJava_shouldAppearBeforeBootRun() {
        int compileIdx = -1;
        int bootRunIdx = -1;
        for (int i = 0; i < bootRunDryRunOutput.size(); i++) {
            String line = bootRunDryRunOutput.get(i);
            if (line.contains(":configuration:compileJava") && compileIdx == -1) {
                compileIdx = i;
            }
            if (line.contains(":configuration:bootRun") && bootRunIdx == -1) {
                bootRunIdx = i;
            }
        }
        if (compileIdx >= 0 && bootRunIdx >= 0) {
            assertThat(compileIdx)
                    .as("compileJava must execute before bootRun in the task graph — "
                            + "the boot loader needs compiled classes to find the main class")
                    .isLessThan(bootRunIdx);
        }
    }

    @Test
    void bootRunDryRun_shouldIncludeClassesTask() {
        boolean hasClasses = bootRunDryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:classes"));
        assertThat(hasClasses)
                .as("bootRun task graph must include the 'classes' lifecycle task — "
                        + "bootRun depends on classes to ensure all compilation is complete")
                .isTrue();
    }

    @Test
    void bootRunDryRun_shouldNotIncludeTestTasks() {
        boolean hasTestTasks = bootRunDryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:test")
                        || line.contains(":configuration:compileTestJava"));
        assertThat(hasTestTasks)
                .as("bootRun task graph must NOT include test tasks — "
                        + "bootRun should only compile and run production code, "
                        + "not execute or compile tests")
                .isFalse();
    }
}
