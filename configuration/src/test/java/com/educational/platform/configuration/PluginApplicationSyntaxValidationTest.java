package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the syntactic correctness of the Spring Boot plugin application
 * in configuration/build.gradle.kts. The PR applies the plugin via
 * {@code alias(libs.plugins.springboot)} inside a {@code plugins {}} block;
 * these tests guard against using incorrect Gradle DSL syntax (e.g. {@code id()}
 * instead of {@code alias()}) and against accidental disabling of boot tasks
 * or overriding of the auto-detected main class.
 */
class PluginApplicationSyntaxValidationTest {

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
    void pluginsBlock_shouldUseAliasSyntax_notIdSyntax() {
        assertThat(buildContent)
                .as("Spring Boot plugin must be applied via alias() in version catalog style, "
                        + "not id() which does not resolve version catalog references")
                .containsPattern("alias\\s*\\(\\s*libs\\.plugins\\.springboot\\s*\\)");
    }

    @Test
    void pluginsBlock_shouldNotUseIdSyntax_forSpringBoot() {
        List<String> pluginLines = buildContent.lines()
                .filter(line -> line.contains("springboot") || line.contains("org.springframework.boot"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        for (String line : pluginLines) {
            assertThat(line)
                    .as("Spring Boot plugin must not be applied via id() syntax — use alias() instead")
                    .doesNotContainPattern("\\bid\\s*\\(");
        }
    }

    @Test
    void buildFile_shouldNotDisableBootRunTask() {
        assertThat(buildContent)
                .as("bootRun task must not be disabled — the configuration module is the "
                        + "application entry point and bootRun is documented in the README")
                .doesNotContainPattern("bootRun.*enabled\\s*=\\s*false");
    }

    @Test
    void buildFile_shouldNotOverrideMainClass() {
        assertThat(buildContent)
                .as("mainClass must not be explicitly overridden — the Spring Boot plugin "
                        + "auto-detects it via @SpringBootApplication annotation")
                .doesNotContainPattern("mainClass\\s*[.=]");
    }

    @Test
    void pluginsBlock_shouldContainExactlyOnePluginApplication() {
        long aliasCount = buildContent.lines()
                .filter(line -> line.trim().startsWith("alias("))
                .count();
        assertThat(aliasCount)
                .as("plugins block must contain exactly one alias() entry (springboot)")
                .isEqualTo(1);
    }

    @Test
    void pluginsBlock_shouldNotApplyPluginWithApplyFalse() {
        assertThat(buildContent)
                .as("Spring Boot plugin must be actively applied — apply false would prevent "
                        + "bootRun/bootJar task registration")
                .doesNotContainPattern("apply\\s*=?\\s*false");
    }

    @Test
    void buildFile_shouldNotConfigureBootJarMainClass() {
        assertThat(buildContent)
                .as("bootJar main class must not be overridden — plugin auto-detects it")
                .doesNotContainPattern("bootJar\\s*\\{[^}]*mainClass");
    }

    @Test
    void buildFile_shouldNotConfigureSpringBootExtension() {
        assertThat(buildContent)
                .as("springBoot extension block should not be present — defaults are sufficient "
                        + "for auto-detection of the main class")
                .doesNotContainPattern("springBoot\\s*\\{");
    }
}
