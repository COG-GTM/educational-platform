package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the Gradle build configuration files modified by the PR.
 * Guards against accidental removal of the Spring Boot plugin declaration
 * from libs.versions.toml or the plugin application / starter-test dependency
 * from configuration/build.gradle.kts.
 */
class GradleBuildFileValidationTest {

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
    void libsVersionsToml_shouldDeclareSpringVersion() throws IOException {
        String content = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
        assertThat(content)
                .as("libs.versions.toml must declare the spring version used by both BOM and plugin")
                .containsPattern("spring\\s*=\\s*\"[\\d.]+\"");
    }

    @Test
    void libsVersionsToml_shouldContainSpringbootPluginEntry() throws IOException {
        String content = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
        assertThat(content)
                .as("libs.versions.toml must declare the springboot plugin")
                .contains("springboot");
    }

    @Test
    void libsVersionsToml_springbootPlugin_shouldReferenceOrgSpringframeworkBoot() throws IOException {
        String content = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
        assertThat(content)
                .as("springboot plugin must use the org.springframework.boot plugin ID")
                .contains("org.springframework.boot");
    }

    @Test
    void libsVersionsToml_springbootPlugin_shouldUseSpringVersionRef() throws IOException {
        String content = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
        assertThat(content)
                .as("springboot plugin must reference version.ref = \"spring\" for BOM alignment")
                .containsPattern("springboot.*version\\.ref.*=.*\"spring\"");
    }

    @Test
    void configurationBuildGradle_shouldApplySpringBootPlugin() throws IOException {
        String content = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(content)
                .as("configuration/build.gradle.kts must apply the Spring Boot plugin via alias")
                .contains("libs.plugins.springboot");
    }

    @Test
    void configurationBuildGradle_shouldDeclareSpringBootStarterTestDependency() throws IOException {
        String content = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(content)
                .as("configuration/build.gradle.kts must include spring-boot-starter-test")
                .contains("spring-boot-starter-test");
    }

    @Test
    void configurationBuildGradle_starterTest_shouldBeTestScoped() throws IOException {
        String content = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(content)
                .as("spring-boot-starter-test must be declared as testImplementation, not implementation")
                .containsPattern("testImplementation.*spring-boot-starter-test");
    }
}
