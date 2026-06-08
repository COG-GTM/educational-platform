package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin is correctly configured to register
 * boot tasks without explicit task declarations or overrides. The PR applies
 * the plugin via {@code alias(libs.plugins.springboot)} with no additional
 * task configuration; the plugin auto-registers bootRun and bootJar tasks
 * using convention-based defaults. Explicit task declarations would shadow
 * the plugin's conventions and risk misconfiguration.
 * <p>
 * Complements {@link BootRunTaskRequirementsValidationTest} (which validates
 * Gradle task output from build scripts), {@link BootJarConfigurationValidationTest}
 * (which validates bootJar content), and
 * {@link PluginApplicationSyntaxValidationTest} (which validates alias syntax).
 * This test focuses on the absence of task-level overrides in the build script.
 */
class BootPluginTaskRegistrationTest {

    private static String buildContent;

    @BeforeAll
    static void loadBuildFile() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        buildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void buildFile_shouldNotExplicitlyConfigureBootRunTask() {
        assertThat(buildContent)
                .as("bootRun task must NOT be explicitly configured — "
                        + "the Spring Boot plugin registers it automatically with "
                        + "convention-based defaults; overriding would risk breaking "
                        + "classpath and main-class resolution")
                .doesNotContainPattern("tasks\\.named.*bootRun")
                .doesNotContainPattern("tasks\\.withType.*BootRun")
                .doesNotContainPattern("tasks\\.register.*bootRun");
    }

    @Test
    void buildFile_shouldNotExplicitlyConfigureBootJarTask() {
        assertThat(buildContent)
                .as("bootJar task must NOT be explicitly configured — "
                        + "the plugin auto-configures it with the detected main class; "
                        + "overriding would bypass auto-detection conventions")
                .doesNotContainPattern("tasks\\.named.*bootJar")
                .doesNotContainPattern("tasks\\.withType.*BootJar")
                .doesNotContainPattern("tasks\\.register.*bootJar");
    }

    @Test
    void buildFile_shouldNotDisableStandardJarTask() {
        assertThat(buildContent)
                .as("Standard jar task must NOT be explicitly disabled — "
                        + "the Spring Boot plugin manages the interaction between "
                        + "jar and bootJar tasks automatically")
                .doesNotContainPattern("tasks\\.jar.*enabled.*false")
                .doesNotContainPattern("jar\\s*\\{[^}]*enabled\\s*=\\s*false");
    }

    @Test
    void buildFile_shouldNotDeclareMainClassAttribute() {
        assertThat(buildContent)
                .as("Main class must NOT be explicitly set — the Spring Boot plugin "
                        + "auto-detects it from @SpringBootApplication; setting it manually "
                        + "risks desynchronization from the actual application class")
                .doesNotContain("mainClass")
                .doesNotContain("Main-Class")
                .doesNotContain("Start-Class");
    }

    @Test
    void buildFile_shouldNotConfigureApplicationPlugin() {
        assertThat(buildContent)
                .as("The Gradle 'application' plugin must NOT be applied — "
                        + "it conflicts with the Spring Boot plugin's bootRun task "
                        + "by also registering a run task with its own mainClass configuration")
                .doesNotContain("\"application\"")
                .doesNotContain("application {");
    }

    @Test
    void buildFile_shouldNotConfigureDistributionPlugin() {
        assertThat(buildContent)
                .as("The Gradle 'distribution' plugin must NOT be applied — "
                        + "the Spring Boot plugin's bootJar already produces a fat JAR; "
                        + "distribution would create redundant ZIP/TAR artifacts")
                .doesNotContain("\"distribution\"");
    }
}
