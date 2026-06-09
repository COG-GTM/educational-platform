package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper infrastructure is present and consistent.
 * The Spring Boot plugin relies on the Gradle wrapper (gradlew) to execute
 * bootRun/bootJar. If the wrapper is missing or misconfigured, the documented
 * command `./gradlew :configuration:bootRun` would fail.
 */
class GradleWrapperConsistencyTest {

    private static Path projectRoot;

    @BeforeAll
    static void findProjectRoot() {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
    }

    @Test
    void gradlewScript_shouldExist() {
        assertThat(Files.exists(projectRoot.resolve("gradlew")))
                .as("gradlew script must exist for ./gradlew :configuration:bootRun")
                .isTrue();
    }

    @Test
    void gradlewScript_shouldBeExecutable() {
        Path gradlew = projectRoot.resolve("gradlew");
        assertThat(Files.isExecutable(gradlew))
                .as("gradlew must be executable for direct invocation")
                .isTrue();
    }

    @Test
    void gradleWrapperProperties_shouldExist() {
        assertThat(Files.exists(projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties")))
                .as("gradle-wrapper.properties must exist for wrapper version resolution")
                .isTrue();
    }

    @Test
    void gradleWrapperProperties_shouldSpecifyDistributionUrl() throws IOException {
        String content = Files.readString(projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties"));
        assertThat(content)
                .as("gradle-wrapper.properties must declare distributionUrl for reproducible builds")
                .containsPattern("distributionUrl=.*gradle.*\\.zip");
    }

    @Test
    void gradleWrapperJar_shouldExist() {
        assertThat(Files.exists(projectRoot.resolve("gradle/wrapper/gradle-wrapper.jar")))
                .as("gradle-wrapper.jar must exist for wrapper bootstrap")
                .isTrue();
    }

    @Test
    void gradleWrapperProperties_shouldUseHttpsDistribution() throws IOException {
        String content = Files.readString(projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties"));
        assertThat(content)
                .as("Gradle distribution URL must use HTTPS for secure downloads")
                .containsPattern("https\\\\?://");
    }

    @Test
    void settingsGradle_shouldExist_forMultiModuleProject() {
        assertThat(Files.exists(projectRoot.resolve("settings.gradle.kts")))
                .as("settings.gradle.kts must exist for multi-module project configuration")
                .isTrue();
    }

    @Test
    void settingsGradle_shouldReferenceConfigurationModule() throws IOException {
        String content = Files.readString(projectRoot.resolve("settings.gradle.kts"));
        assertThat(content)
                .as("settings.gradle.kts must include the configuration module where Spring Boot plugin is applied")
                .contains("configuration");
    }
}
