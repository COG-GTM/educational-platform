package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the co-dependency invariant between the Spring Boot plugin
 * and the spring-boot-starter-test dependency in configuration/build.gradle.kts.
 * The plugin enables bootRun/bootJar tasks while starter-test provides
 * {@code @SpringBootTest} and test slice support. If either is present
 * without the other, the configuration module's test infrastructure or
 * boot task capability degrades silently:
 * <ul>
 *   <li>Plugin without starter-test: bootRun works but @SpringBootTest
 *       context loading fails at test time</li>
 *   <li>Starter-test without plugin: tests compile but bootRun/bootJar
 *       tasks are missing, breaking the dev workflow</li>
 * </ul>
 * Complements {@link PluginApplicationSyntaxValidationTest} (plugin syntax)
 * and {@link StarterTestDependencyNotationValidationTest} (starter-test format).
 */
class PluginStarterTestCoDependencyTest {

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
    void springBootPlugin_andStarterTest_shouldBothBePresent() {
        boolean hasPlugin = buildContent.contains("libs.plugins.springboot");
        boolean hasStarterTest = buildContent.contains("spring-boot-starter-test");
        assertThat(hasPlugin && hasStarterTest)
                .as("The Spring Boot plugin and spring-boot-starter-test must both be present — "
                        + "the plugin provides bootRun/bootJar tasks, and starter-test provides "
                        + "@SpringBootTest context loading for integration tests")
                .isTrue();
    }

    @Test
    void starterTest_shouldBeInTestScope_notImplementation() {
        List<String> starterTestLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(starterTestLines)
                .as("spring-boot-starter-test must be declared at least once")
                .isNotEmpty();
        for (String line : starterTestLines) {
            assertThat(line.trim())
                    .as("starter-test must use testImplementation scope, not implementation — "
                            + "test infrastructure must not leak into the production classpath "
                            + "of the bootJar artifact")
                    .startsWith("testImplementation(");
        }
    }

    @Test
    void starterWeb_shouldBeInImplementationScope_notTest() {
        List<String> starterWebLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(starterWebLines)
                .as("spring-boot-starter-web must be declared for bootRun HTTP serving")
                .isNotEmpty();
        for (String line : starterWebLines) {
            assertThat(line.trim())
                    .as("starter-web must use implementation scope — "
                            + "it is needed at runtime for the embedded server in bootRun")
                    .startsWith("implementation(");
        }
    }

    @Test
    void pluginAndStarterWeb_shouldBothExistForBootRun() {
        assertThat(buildContent)
                .as("The Spring Boot plugin requires starter-web for bootRun to start "
                        + "an embedded HTTP server")
                .contains("libs.plugins.springboot")
                .contains("spring-boot-starter-web");
    }

    @Test
    void pluginBlock_shouldPrecedeStarterTestDeclaration() {
        int pluginIdx = buildContent.indexOf("libs.plugins.springboot");
        int starterTestIdx = buildContent.indexOf("spring-boot-starter-test");
        assertThat(pluginIdx)
                .as("Plugin declaration must appear before starter-test dependency — "
                        + "Gradle requires the plugins block at the top of the build file")
                .isLessThan(starterTestIdx);
    }
}
