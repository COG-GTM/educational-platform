package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards that the root build.gradle.kts does NOT apply the Spring Boot plugin.
 * LibraryModuleBuildIsolationTest validates library sub-modules; this test
 * explicitly validates the root project. If the Spring Boot plugin were applied
 * at the root level (e.g., via allprojects or subprojects), it would:
 * <ul>
 *   <li>Register bootJar/bootRun tasks for every sub-module, breaking library JARs</li>
 *   <li>Cause "no main class found" errors in modules without @SpringBootApplication</li>
 *   <li>Conflict with the explicit plugin application in configuration/build.gradle.kts</li>
 * </ul>
 */
class RootBuildFileSpringBootPluginExclusionTest {

    private static String rootBuildContent;

    @BeforeAll
    static void loadRootBuildFile() throws IOException {
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
    void rootBuild_shouldNotApplySpringBootPluginViaAlias() {
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts must NOT apply the Spring Boot plugin via alias() — "
                        + "only the configuration module should apply it")
                .doesNotContain("libs.plugins.springboot");
    }

    @Test
    void rootBuild_shouldNotApplySpringBootPluginViaId() {
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts must NOT apply org.springframework.boot via id() syntax")
                .doesNotContainPattern("id\\s*\\(\\s*\"org\\.springframework\\.boot\"");
    }

    @Test
    void rootBuild_shouldNotApplySpringBootInAllprojects() {
        assertThat(rootBuildContent)
                .as("Root build must NOT apply Spring Boot plugin in allprojects block — "
                        + "this would register bootJar/bootRun in every module")
                .doesNotContainPattern("allprojects\\s*\\{[^}]*springboot");
    }

    @Test
    void rootBuild_shouldNotApplySpringBootInSubprojects() {
        assertThat(rootBuildContent)
                .as("Root build must NOT apply Spring Boot plugin in subprojects block — "
                        + "each module that needs it should apply it explicitly")
                .doesNotContainPattern("subprojects\\s*\\{[^}]*springboot");
    }

    @Test
    void rootBuild_shouldNotConfigureBootRunTask() {
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts must NOT configure bootRun — "
                        + "this task only makes sense in the configuration module")
                .doesNotContainPattern("bootRun\\s*\\{");
    }

    @Test
    void rootBuild_shouldNotConfigureBootJarTask() {
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts must NOT configure bootJar — "
                        + "only the configuration module produces the executable JAR")
                .doesNotContainPattern("bootJar\\s*\\{");
    }
}
