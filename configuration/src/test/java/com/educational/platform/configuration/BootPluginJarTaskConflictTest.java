package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against Gradle task configuration that would conflict with the
 * Spring Boot plugin. When the Spring Boot plugin is applied, it registers
 * bootJar and bootRun tasks and disables the standard jar task by default.
 * Explicit jar task configuration, bootBuildImage overrides, or conflicting
 * application plugin usage could break this behavior.
 */
class BootPluginJarTaskConflictTest {

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
    void buildFile_shouldNotExplicitlyConfigureJarTask() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT configure the 'jar' task explicitly — "
                        + "the Spring Boot plugin disables the standard jar task in favor of bootJar; "
                        + "re-enabling it would produce a non-executable JAR alongside bootJar")
                .doesNotContainPattern("\\bjar\\s*\\{");
    }

    @Test
    void buildFile_shouldNotApplyApplicationPlugin() {
        assertThat(buildContent)
                .as("The 'application' plugin must NOT be applied alongside the Spring Boot plugin — "
                        + "both create distribution tasks and the Spring Boot plugin already handles "
                        + "main class detection and distribution packaging")
                .doesNotContain("\"application\"")
                .doesNotContainPattern("plugin.*application");
    }

    @Test
    void buildFile_shouldNotConfigureBootBuildImageTask() {
        assertThat(buildContent)
                .as("bootBuildImage task should not be configured — "
                        + "OCI image creation is not part of the current deployment strategy")
                .doesNotContainPattern("bootBuildImage\\s*\\{");
    }

    @Test
    void buildFile_shouldNotConfigureBootRunJvmArgs() {
        assertThat(buildContent)
                .as("bootRun JVM args should not be hardcoded in the build file — "
                        + "JVM configuration should be passed via command line for environment flexibility")
                .doesNotContainPattern("bootRun\\s*\\{[^}]*jvmArgs");
    }

    @Test
    void buildFile_shouldNotDeclareMainClassName() {
        assertThat(buildContent)
                .as("Main class name must not be declared in any task configuration — "
                        + "the Spring Boot plugin auto-detects it via @SpringBootApplication")
                .doesNotContainPattern("mainClassName\\s*=")
                .doesNotContainPattern("mainClass\\.set\\(");
    }

    @Test
    void buildFile_shouldNotConfigureDistribution() {
        assertThat(buildContent)
                .as("Distribution configuration must not be present — "
                        + "the Spring Boot plugin handles fat JAR packaging via bootJar")
                .doesNotContainPattern("distributions\\s*\\{");
    }
}
