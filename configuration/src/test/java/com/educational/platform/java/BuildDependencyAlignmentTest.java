package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the configuration module's build.gradle.kts declares all
 * required test dependencies for the Java 26 upgrade verification tests.
 * <p>
 * The Java 26 upgrade added assertj-core and junit-jupiter-engine as
 * explicit dependencies. These tests prevent accidental removal.
 */
public class BuildDependencyAlignmentTest {

    @Test
    void configBuild_shouldDeclare_junitJupiterApi() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare junit-jupiter-api as testImplementation")
                .contains("testImplementation")
                .contains("junit-jupiter-api");
    }

    @Test
    void configBuild_shouldDeclare_junitJupiterParams() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare junit-jupiter-params as testImplementation for parameterized tests")
                .contains("junit-jupiter-params");
    }

    @Test
    void configBuild_shouldDeclare_junitJupiterEngine_asRuntimeOnly() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare junit-jupiter-engine as testRuntimeOnly")
                .contains("testRuntimeOnly")
                .contains("junit-jupiter-engine");
    }

    @Test
    void configBuild_shouldDeclare_assertjCore() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare assertj-core as testImplementation")
                .contains("assertj-core");
    }

    @Test
    void configBuild_shouldDeclare_archunitJunit5() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare archunit-junit5 as testImplementation")
                .contains("archunit-junit5");
    }

    @Test
    void configBuild_shouldDeclare_mockitoJunitJupiter() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare mockito-junit-jupiter as testImplementation")
                .contains("mockito-junit-jupiter");
    }

    @Test
    void configBuild_shouldUse_versionCatalogReferences() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should reference libs.versions.archunit for ArchUnit version")
                .contains("libs.versions.archunit");

        assertThat(content)
                .as("Should reference libs.versions.mockito for Mockito version")
                .contains("libs.versions.mockito");

        assertThat(content)
                .as("Should reference libs.versions.assertj for AssertJ version")
                .contains("libs.versions.assertj");
    }

    @Test
    void configBuild_shouldUse_junitPlatform() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should configure JUnit Platform for test execution")
                .contains("useJUnitPlatform()");
    }

    @Test
    void configBuild_shouldDeclare_junitPlatformEngine() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare junit-platform-engine")
                .contains("junit-platform-engine");
    }

    @Test
    void configBuild_shouldDeclare_junitPlatformLauncher() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("Should declare junit-platform-launcher")
                .contains("junit-platform-launcher");
    }

    @Test
    void configBuild_shouldNotHave_hardcodedVersions_forCatalogDeps() throws IOException {
        String content = readConfigBuildGradle();
        List<String> lines = content.lines().toList();

        // Lines with archunit, mockito, assertj should use libs.versions, not hardcoded strings
        for (String line : lines) {
            if (line.contains("archunit") && line.contains("testImplementation")) {
                assertThat(line)
                        .as("archunit dependency should use version catalog, not hardcoded")
                        .contains("libs.versions.archunit");
            }
            if (line.contains("mockito") && line.contains("testImplementation")) {
                assertThat(line)
                        .as("mockito dependency should use version catalog, not hardcoded")
                        .contains("libs.versions.mockito");
            }
            if (line.contains("assertj") && line.contains("testImplementation")) {
                assertThat(line)
                        .as("assertj dependency should use version catalog, not hardcoded")
                        .contains("libs.versions.assertj");
            }
        }
    }

    @Test
    void configBuild_shouldDeclare_allSubprojectDependencies() throws IOException {
        String content = readConfigBuildGradle();

        // The configuration module aggregates all bounded contexts
        assertThat(content)
                .as("Should include users module")
                .contains(":users:");

        assertThat(content)
                .as("Should include administration module")
                .contains(":administration:");

        assertThat(content)
                .as("Should include course-enrollments module")
                .contains(":course-enrollments:");

        assertThat(content)
                .as("Should include course-reviews module")
                .contains(":course-reviews:");

        assertThat(content)
                .as("Should include courses module")
                .contains(":courses:");
    }

    private String readConfigBuildGradle() throws IOException {
        Path configBuild = findProjectRoot().resolve("configuration/build.gradle.kts");
        return Files.readString(configBuild);
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
