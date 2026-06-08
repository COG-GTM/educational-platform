package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Gradle version catalog (libs.versions.toml) contains
 * dependency versions compatible with Java 26.
 * <p>
 * This prevents accidental downgrades of dependencies that require minimum
 * versions for Java 26 bytecode support.
 */
public class BuildConfigVersionsTest {

    @Test
    void versionCatalog_shouldExist() {
        Path catalog = findVersionCatalog();

        assertThat(catalog)
                .as("Gradle version catalog should exist")
                .exists();
    }

    @Test
    void versionCatalog_archunit_shouldBe_142OrHigher() throws IOException {
        Path catalog = findVersionCatalog();
        String content = Files.readString(catalog);

        assertThat(content)
                .as("Version catalog should define archunit version")
                .contains("archunit");

        // ArchUnit 1.4.2+ required for Java 26 class file support
        assertThat(content)
                .as("ArchUnit version should be 1.4.2 or higher for Java 26 compatibility")
                .containsPattern("archunit\\s*=\\s*\"1\\.4\\.[2-9]\"")
                .doesNotContainPattern("archunit\\s*=\\s*\"1\\.4\\.[01]\"");
    }

    @Test
    void versionCatalog_shouldDefine_allTestDependencyVersions() throws IOException {
        Path catalog = findVersionCatalog();
        String content = Files.readString(catalog);

        assertThat(content)
                .as("Version catalog should define key test dependency versions")
                .contains("mockito")
                .contains("assertj")
                .contains("archunit");
    }

    @Test
    void buildGradle_shouldTarget_java26() throws IOException {
        Path buildGradle = findBuildGradle();
        String content = Files.readString(buildGradle);

        assertThat(content)
                .as("Root build.gradle.kts should target Java 26")
                .contains("VERSION_26");

        assertThat(content)
                .as("Root build.gradle.kts should not reference old Java 25 target")
                .doesNotContain("VERSION_25");
    }

    private Path findVersionCatalog() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("gradle/libs.versions.toml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Paths.get("gradle/libs.versions.toml");
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

    private Path findBuildGradle() {
        return findProjectRoot().resolve("build.gradle.kts");
    }
}
