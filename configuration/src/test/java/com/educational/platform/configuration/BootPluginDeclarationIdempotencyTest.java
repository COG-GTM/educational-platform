package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin is declared exactly once across
 * the entire multi-module build. The PR applies the plugin in the
 * configuration module via {@code alias(libs.plugins.springboot)};
 * if it is also applied (via {@code id()} or {@code alias()}) in another
 * module's build file, Gradle will attempt to repackage that module as
 * a bootable JAR, which breaks library module compilation and produces
 * conflicting main-class detection.
 * <p>
 * Complements {@link RootBuildFileSpringBootPluginExclusionTest} (guards
 * root build.gradle.kts) and {@link LibraryModuleBuildIsolationTest}
 * (guards specific library modules). This test provides a comprehensive
 * sweep across ALL build.gradle.kts files in the project.
 */
class BootPluginDeclarationIdempotencyTest {

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
    void springbootPlugin_shouldBeAppliedInExactlyOneBuildFile() throws IOException {
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .filter(p -> !p.toString().contains(".gradle/"))
                    .toList();

            List<Path> filesWithSpringBootPlugin = buildFiles.stream()
                    .filter(p -> {
                        try {
                            String content = Files.readString(p);
                            // Check for version catalog alias or id() plugin application only,
                            // NOT BOM imports which also reference "org.springframework.boot"
                            boolean hasAlias = content.contains("libs.plugins.springboot");
                            boolean hasIdPlugin = content.lines()
                                    .filter(line -> !line.trim().startsWith("//"))
                                    .anyMatch(line -> line.matches(
                                            ".*\\bid\\s*\\(\\s*\"org\\.springframework\\.boot\".*"));
                            return hasAlias || hasIdPlugin;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();

            assertThat(filesWithSpringBootPlugin)
                    .as("The Spring Boot plugin must be applied in exactly one build file "
                            + "(configuration/build.gradle.kts) — applying it in multiple modules "
                            + "causes conflicting bootJar/bootRun task registration")
                    .hasSize(1);

            assertThat(filesWithSpringBootPlugin.getFirst().toString())
                    .as("The Spring Boot plugin must be in configuration module only")
                    .contains("configuration");
        }
    }

    @Test
    void configurationBuildFile_shouldDeclareSpringBootPluginExactlyOnce() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("configuration/build.gradle.kts"));
        long aliasCount = content.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("libs.plugins.springboot"))
                .count();
        assertThat(aliasCount)
                .as("Spring Boot plugin alias must appear exactly once in configuration build file — "
                        + "duplicate declaration would cause Gradle to fail with 'plugin already applied'")
                .isEqualTo(1);
    }

    @Test
    void noBuildFile_shouldUseIdSyntaxForSpringBoot() throws IOException {
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .filter(p -> !p.toString().contains(".gradle/"))
                    .toList();

            for (Path buildFile : buildFiles) {
                String content = Files.readString(buildFile);
                boolean hasIdSpringBoot = content.lines()
                        .filter(line -> !line.trim().startsWith("//"))
                        .anyMatch(line -> line.matches(
                                ".*\\bid\\s*\\(\\s*\"org\\.springframework\\.boot\".*"));
                assertThat(hasIdSpringBoot)
                        .as("Build file %s must NOT use id() syntax for the Spring Boot plugin — "
                                + "only alias(libs.plugins.springboot) is allowed to ensure "
                                + "version catalog management", projectRoot.relativize(buildFile))
                        .isFalse();
            }
        }
    }
}
