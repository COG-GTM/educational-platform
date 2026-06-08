package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the application entry point class is the only production
 * Java source file in the com.educational.platform base package directory.
 * The Spring Boot plugin auto-detects the main class by scanning for
 * @SpringBootApplication; placing additional classes in the base package
 * could interfere with component scanning scope and makes the composition
 * root harder to reason about. Domain logic belongs in bounded-context
 * sub-packages (e.g., com.educational.platform.courses).
 * <p>
 * Complements {@link com.educational.platform.configuration.ConfigurationModuleOnlyBootEntryPointTest}
 * which guards against @SpringBootApplication in OTHER modules.
 */
class ApplicationClassPackageExclusivenessTest {

    @Test
    void basePackage_shouldContainOnlyTheApplicationClass() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir).isNotNull();

        Path basePackageDir = dir.resolve(
                "configuration/src/main/java/com/educational/platform");
        assertThat(Files.exists(basePackageDir))
                .as("Base package directory must exist")
                .isTrue();

        // Only list direct children (not recursive) — subdirectories are expected
        try (Stream<Path> files = Files.list(basePackageDir)) {
            List<Path> javaFiles = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
            assertThat(javaFiles)
                    .as("Base package com.educational.platform in configuration/src/main must "
                            + "contain only EducationalPlatformApplication.java — other production "
                            + "classes belong in bounded-context sub-packages")
                    .hasSize(1);
            assertThat(javaFiles.getFirst().getFileName().toString())
                    .isEqualTo("EducationalPlatformApplication.java");
        }
    }

    @Test
    void applicationClass_shouldBeInCorrectSourceSet() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir).isNotNull();

        Path mainSource = dir.resolve(
                "configuration/src/main/java/com/educational/platform/EducationalPlatformApplication.java");
        Path testSource = dir.resolve(
                "configuration/src/test/java/com/educational/platform/EducationalPlatformApplication.java");

        assertThat(Files.exists(mainSource))
                .as("Application class must exist in src/main/java source set")
                .isTrue();
        assertThat(Files.exists(testSource))
                .as("Application class must NOT exist in src/test/java — "
                        + "the Spring Boot plugin requires the main class in the production source set")
                .isFalse();
    }
}
