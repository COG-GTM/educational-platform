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
 * Validates that the Spring Boot plugin can auto-detect the application main
 * class without explicit configuration by running the bootJar task in dry-run
 * mode. Existing tests validate the class structure reflectively
 * ({@link com.educational.platform.application.EducationalPlatformApplicationMainTest})
 * and check that no mainClass override exists in the build file
 * ({@link PluginApplicationSyntaxValidationTest}); this test verifies the actual
 * Gradle plugin behavior by attempting to execute the bootJar task planning phase.
 * <p>
 * If the Spring Boot plugin cannot find a class annotated with
 * {@code @SpringBootApplication} or finds multiple candidates, the dry-run
 * will fail with "Unable to find a suitable main class" error.
 */
class BootPluginMainClassResolutionIntegrationTest {

    private static Path projectRoot;
    private static List<String> dryRunOutput;
    private static int exitCode;

    @BeforeAll
    static void runBootJarDryRun() throws IOException, InterruptedException {
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
                wrapper, ":configuration:bootJar", "--dry-run", "-q")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            dryRunOutput = reader.lines().collect(Collectors.toList());
        }
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        assertThat(finished)
                .as("Gradle bootJar --dry-run must complete within 120 seconds")
                .isTrue();
        exitCode = process.exitValue();
    }

    @Test
    void bootJarDryRun_shouldSucceed() {
        assertThat(exitCode)
                .as("bootJar --dry-run must succeed (exit code 0) — "
                        + "failure indicates the Spring Boot plugin cannot resolve the main class "
                        + "or the task graph has unresolvable dependencies")
                .isEqualTo(0);
    }

    @Test
    void bootJarDryRun_shouldNotReportMainClassError() {
        boolean hasMainClassError = dryRunOutput.stream()
                .anyMatch(line -> line.contains("Unable to find a suitable main class")
                        || line.contains("mainClass"));
        assertThat(hasMainClassError)
                .as("bootJar dry-run must NOT report main class resolution issues — "
                        + "the plugin should auto-detect EducationalPlatformApplication")
                .isFalse();
    }

    @Test
    void bootJarDryRun_shouldIncludeCompileJavaTask() {
        boolean hasCompileJava = dryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:compileJava"));
        assertThat(hasCompileJava)
                .as("bootJar task graph must include compileJava — "
                        + "sources must be compiled before packaging into the boot JAR")
                .isTrue();
    }

    @Test
    void bootJarDryRun_shouldIncludeProcessResourcesTask() {
        boolean hasProcessResources = dryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:processResources"));
        assertThat(hasProcessResources)
                .as("bootJar task graph must include processResources — "
                        + "application.properties and other resources must be packaged")
                .isTrue();
    }

    @Test
    void bootJarDryRun_shouldIncludeBootJarTask() {
        boolean hasBootJar = dryRunOutput.stream()
                .anyMatch(line -> line.contains(":configuration:bootJar"));
        assertThat(hasBootJar)
                .as("bootJar task must appear in the dry-run output — "
                        + "confirms the Spring Boot plugin registered the task correctly")
                .isTrue();
    }
}
