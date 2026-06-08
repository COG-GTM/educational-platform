package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the structural integrity of the root build.gradle.kts after
 * the Java 25 to Java 26 version bump.
 * <p>
 * The PR only changed the {@code sourceCompatibility} and
 * {@code targetCompatibility} lines, but an accidental edit could break
 * the surrounding structure (allprojects, subprojects, plugins,
 * dependency management BOM). This test ensures all key structural
 * elements remain intact.
 */
public class RootBuildScriptStructureTest {

    @Test
    void buildScript_shouldDeclare_javaPlugin() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should apply the java plugin")
                .containsPattern("\\bjava\\b");
    }

    @Test
    void buildScript_shouldHave_allprojectsBlock() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should have an allprojects block")
                .contains("allprojects");
    }

    @Test
    void buildScript_shouldHave_subprojectsBlock() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should have a subprojects block")
                .contains("subprojects");
    }

    @Test
    void buildScript_allprojects_shouldDefine_groupAndVersion() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should define group")
                .contains("group = \"com.educational.platform\"");

        assertThat(content)
                .as("Root build script should define version")
                .containsPattern("version\\s*=");
    }

    @Test
    void buildScript_shouldConfigureMavenCentral() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should use mavenCentral() repository")
                .contains("mavenCentral()");
    }

    @Test
    void buildScript_subprojects_shouldApply_requiredPlugins() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Subprojects should apply java plugin")
                .containsPattern("plugin\\(\"java\"\\)");

        assertThat(content)
                .as("Subprojects should apply dependency-management plugin")
                .contains("io.spring.dependency-management");

        assertThat(content)
                .as("Subprojects should apply java-library plugin")
                .contains("java-library");
    }

    @Test
    void buildScript_shouldConfigure_dependencyManagementBom() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should import Spring Boot BOM")
                .contains("spring-boot-dependencies");

        assertThat(content)
                .as("Root build script should reference spring version from catalog")
                .contains("libs.versions.spring");
    }

    @Test
    void buildScript_shouldUse_versionCatalogAliasForPlugin() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build script should use version catalog alias for plugins")
                .contains("alias(libs.plugins.springdependencies)");
    }

    @Test
    void buildScript_sourceAndTarget_shouldBothBeJava26() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("sourceCompatibility should be VERSION_26")
                .contains("sourceCompatibility = JavaVersion.VERSION_26");

        assertThat(content)
                .as("targetCompatibility should be VERSION_26")
                .contains("targetCompatibility = JavaVersion.VERSION_26");
    }

    private String readRootBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("build.gradle.kts"));
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
