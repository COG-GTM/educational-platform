package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Java version declared in the root build.gradle.kts is
 * compatible with the Spring Boot 4.x plugin applied in the configuration
 * module. Spring Boot 4.0 requires Java 17+ and the project uses Java 25.
 * These tests guard against downgrading the Java version below the minimum
 * required by Spring Boot, or accidentally removing the version declaration,
 * which would cause bootRun to fail with an UnsupportedClassVersionError.
 */
class RootBuildJavaVersionCompatibilityTest {

    private static String rootBuildContent;

    @BeforeAll
    static void loadRootBuild() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        rootBuildContent = Files.readString(dir.resolve("build.gradle.kts"));
    }

    @Test
    void rootBuild_shouldDeclareSourceCompatibility() {
        assertThat(rootBuildContent)
                .as("Root build must declare sourceCompatibility for consistent compilation "
                        + "across all modules including the configuration module with the Spring Boot plugin")
                .contains("sourceCompatibility");
    }

    @Test
    void rootBuild_shouldDeclareTargetCompatibility() {
        assertThat(rootBuildContent)
                .as("Root build must declare targetCompatibility to match sourceCompatibility")
                .contains("targetCompatibility");
    }

    @Test
    void rootBuild_sourceCompatibility_shouldBeJava17OrHigher() {
        assertThat(rootBuildContent)
                .as("Java version must be 17 or higher for Spring Boot 4.x compatibility — "
                        + "the project uses Java 25 (VERSION_25)")
                .containsPattern("VERSION_(1[7-9]|[2-9]\\d)");
    }

    @Test
    void rootBuild_javaVersion_shouldBeInsideSubprojectsBlock() {
        int subprojectsIdx = rootBuildContent.indexOf("subprojects");
        int javaBlockIdx = rootBuildContent.indexOf("java {");
        assertThat(subprojectsIdx)
                .as("Root build must have a subprojects block")
                .isGreaterThanOrEqualTo(0);
        assertThat(javaBlockIdx)
                .as("Java block must be present in root build")
                .isGreaterThanOrEqualTo(0);
        assertThat(javaBlockIdx)
                .as("Java version configuration must be inside subprojects block "
                        + "so all modules (including configuration) inherit it")
                .isGreaterThan(subprojectsIdx);
    }

    @Test
    void runtimeJavaVersion_shouldBe17OrHigher() {
        int runtimeVersion = Runtime.version().feature();
        assertThat(runtimeVersion)
                .as("Runtime JVM must be Java 17+ for Spring Boot 4.x — current: %d", runtimeVersion)
                .isGreaterThanOrEqualTo(17);
    }
}
