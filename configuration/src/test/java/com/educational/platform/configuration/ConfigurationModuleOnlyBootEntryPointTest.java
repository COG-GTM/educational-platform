package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that domain modules do not contain any {@code main} methods or
 * {@code @SpringBootApplication} annotations in their source code. The Spring
 * Boot plugin auto-detects the main class by scanning for
 * {@code @SpringBootApplication}; if a domain module contains one, it would
 * cause ambiguous main-class detection in multi-module builds, even though the
 * plugin is only applied in the configuration module. A stray main method in
 * a domain module could be accidentally invoked instead of the composition root.
 * <p>
 * Complements {@link UniqueSpringBootApplicationTest} (ArchUnit classpath-level
 * check) by guarding at the source-file level across ALL modules, including
 * modules not on the ArchUnit analysis classpath.
 */
class ConfigurationModuleOnlyBootEntryPointTest {

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

    @ParameterizedTest
    @ValueSource(strings = {
            "courses",
            "administration",
            "course-enrollments",
            "course-reviews",
            "users",
            "common",
            "web"
    })
    void domainModule_shouldNotContainSpringBootApplicationAnnotation(String moduleName)
            throws IOException {
        Path moduleDir = projectRoot.resolve(moduleName);
        if (!Files.exists(moduleDir)) {
            return;
        }
        try (Stream<Path> javaFiles = Files.walk(moduleDir)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("/src/main/"))) {
            List<Path> violatingFiles = javaFiles.filter(p -> {
                try {
                    String content = Files.readString(p);
                    return content.contains("@SpringBootApplication");
                } catch (IOException e) {
                    return false;
                }
            }).toList();
            assertThat(violatingFiles)
                    .as("Module '%s' must NOT contain @SpringBootApplication in production sources — "
                            + "only the configuration module's EducationalPlatformApplication.java "
                            + "should have this annotation to ensure single-entry-point detection "
                            + "by the Spring Boot plugin", moduleName)
                    .isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "courses",
            "administration",
            "course-enrollments",
            "course-reviews",
            "users",
            "common",
            "web"
    })
    void domainModule_shouldNotContainMainMethodInProductionSources(String moduleName)
            throws IOException {
        Path moduleDir = projectRoot.resolve(moduleName);
        if (!Files.exists(moduleDir)) {
            return;
        }
        try (Stream<Path> javaFiles = Files.walk(moduleDir)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("/src/main/"))) {
            List<Path> violatingFiles = javaFiles.filter(p -> {
                try {
                    String content = Files.readString(p);
                    return content.contains("public static void main(String");
                } catch (IOException e) {
                    return false;
                }
            }).toList();
            assertThat(violatingFiles)
                    .as("Module '%s' must NOT contain a main() method in production sources — "
                            + "the configuration module is the sole entry point; stray main methods "
                            + "could be accidentally invoked or confuse IDE run configurations",
                            moduleName)
                    .isEmpty();
        }
    }

    @Test
    void securityModule_shouldNotContainSpringBootApplicationAnnotation() throws IOException {
        Path securityDir = projectRoot.resolve("security");
        if (!Files.exists(securityDir)) {
            return;
        }
        try (Stream<Path> javaFiles = Files.walk(securityDir)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().contains("/src/main/"))) {
            List<Path> violatingFiles = javaFiles.filter(p -> {
                try {
                    String content = Files.readString(p);
                    return content.contains("@SpringBootApplication");
                } catch (IOException e) {
                    return false;
                }
            }).toList();
            assertThat(violatingFiles)
                    .as("Security module must NOT contain @SpringBootApplication — "
                            + "it provides security configuration beans, not an application entry point")
                    .isEmpty();
        }
    }

    @Test
    void configurationModule_shouldContainExactlyOneSpringBootApplication() throws IOException {
        Path configMainJava = projectRoot.resolve("configuration/src/main/java");
        try (Stream<Path> javaFiles = Files.walk(configMainJava)
                .filter(p -> p.toString().endsWith(".java"))) {
            List<Path> appFiles = javaFiles.filter(p -> {
                try {
                    String content = Files.readString(p);
                    return content.contains("@SpringBootApplication");
                } catch (IOException e) {
                    return false;
                }
            }).toList();
            assertThat(appFiles)
                    .as("Configuration module must contain exactly one @SpringBootApplication class "
                            + "for the Spring Boot plugin's main-class auto-detection")
                    .hasSize(1);
            assertThat(appFiles.get(0).getFileName().toString())
                    .as("The @SpringBootApplication class must be EducationalPlatformApplication.java")
                    .isEqualTo("EducationalPlatformApplication.java");
        }
    }
}
