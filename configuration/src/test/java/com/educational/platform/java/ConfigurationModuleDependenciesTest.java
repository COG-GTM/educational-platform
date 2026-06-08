package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the configuration module's build.gradle.kts retains all
 * pre-existing implementation dependencies after the Java 26 upgrade.
 * <p>
 * The Java 26 upgrade added new test dependencies (assertj-core,
 * junit-jupiter-params, junit-jupiter-engine). This test ensures those
 * additions did not accidentally remove or break existing implementation
 * dependencies (spring-boot-starter-web, liquibase-core, subproject refs).
 */
public class ConfigurationModuleDependenciesTest {

    @ParameterizedTest(name = "configuration module should depend on subproject: {0}")
    @ValueSource(strings = {
            ":users:users-application",
            ":users:users-web",
            ":users:users-integration-events",
            ":administration:administration-application",
            ":administration:administration-web",
            ":administration:administration-integration-events",
            ":course-enrollments:course-enrollments-application",
            ":course-enrollments:course-enrollments-web",
            ":course-enrollments:course-enrollments-integration-events",
            ":course-reviews:course-reviews-application",
            ":course-reviews:course-reviews-web",
            ":course-reviews:course-reviews-integration-events",
            ":courses:courses-application",
            ":courses:courses-web",
            ":courses:courses-integration-events",
            ":security:security-config",
            ":web",
            ":common"
    })
    void configBuild_shouldRetain_subprojectDependency(String subproject) throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration build.gradle.kts should still depend on %s", subproject)
                .contains(subproject);
    }

    @Test
    void configBuild_shouldRetain_springBootStarterWeb() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should retain spring-boot-starter-web as implementation dependency")
                .contains("spring-boot-starter-web");
    }

    @Test
    void configBuild_shouldRetain_liquibaseCore() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should retain liquibase-core as implementation dependency")
                .contains("liquibase-core");
    }

    @Test
    void configBuild_shouldSeparate_implementationAndTestDeps() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should have both implementation and testImplementation blocks")
                .contains("implementation(")
                .contains("testImplementation(");
    }

    @Test
    void configBuild_testTask_shouldUseJUnitPlatform() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Test task should be configured to use JUnit Platform")
                .contains("useJUnitPlatform()");
    }

    @Test
    void configBuild_shouldNotDeclare_plugins() throws IOException {
        // The configuration module inherits plugins from the root build script.
        // It should only declare dependencies, not its own plugins block.
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration build.gradle.kts should not have a plugins block")
                .doesNotContain("plugins {");
    }

    private String readConfigBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
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
