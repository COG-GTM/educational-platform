package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that configuration/build.gradle.kts does not declare a
 * {@code springBoot {}} extension block or any explicit boot task
 * configuration. The Spring Boot plugin registers sensible defaults
 * (main class auto-detection, standard bootJar layout, default JVM args);
 * overriding them in the build script creates coupling between the build
 * and application logic.
 * <p>
 * Complements {@link BootRunTaskRequirementsValidationTest} (no mainClass
 * override, no bootRun disabled) and {@link ConfigurationBuildFileStructuralOrderTest}
 * (exactly three top-level blocks). This test specifically targets the
 * springBoot DSL extension and individual task configuration blocks that
 * could silently alter boot plugin behavior.
 */
class BuildFileNoSpringBootExtensionBlockTest {

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
    void buildFile_shouldNotContainSpringBootExtensionBlock() {
        assertThat(buildContent)
                .as("springBoot {} extension block must NOT be present — "
                        + "the plugin's defaults (auto-detected mainClass, standard layout) "
                        + "are correct for this project; overrides create unnecessary coupling")
                .doesNotContainPattern("springBoot\\s*\\{");
    }

    @Test
    void buildFile_shouldNotConfigureBootRunTask() {
        assertThat(buildContent)
                .as("tasks.bootRun configuration must NOT be present — "
                        + "bootRun defaults are sufficient; custom JVM args or system properties "
                        + "should be passed via command-line, not hardcoded in the build script")
                .doesNotContainPattern("tasks\\.bootRun\\s*\\{")
                .doesNotContainPattern("tasks\\.named.*bootRun");
    }

    @Test
    void buildFile_shouldNotConfigureBootJarTask() {
        assertThat(buildContent)
                .as("tasks.bootJar configuration must NOT be present — "
                        + "the default bootJar settings produce a correct executable JAR; "
                        + "customizing layering, excludes, or launch script is not needed")
                .doesNotContainPattern("tasks\\.bootJar\\s*\\{")
                .doesNotContainPattern("tasks\\.named.*bootJar");
    }

    @Test
    void buildFile_shouldNotConfigureBootBuildImage() {
        assertThat(buildContent)
                .as("bootBuildImage task must NOT be configured — "
                        + "container image building is not part of the current deployment model")
                .doesNotContain("bootBuildImage");
    }

    @Test
    void buildFile_shouldNotSetMainClassExplicitly() {
        assertThat(buildContent)
                .as("mainClass must NOT be set explicitly — "
                        + "the Spring Boot plugin auto-detects the @SpringBootApplication class; "
                        + "hardcoding it creates a maintenance burden and can drift out of sync")
                .doesNotContainPattern("mainClass\\s*\\.?\\s*=")
                .doesNotContainPattern("mainClass\\s*\\.set\\s*\\(");
    }

    @Test
    void buildFile_shouldNotConfigureBootWarTask() {
        assertThat(buildContent)
                .as("bootWar task must NOT be configured — "
                        + "the project produces a standalone JAR, not a WAR deployment")
                .doesNotContain("bootWar");
    }
}
