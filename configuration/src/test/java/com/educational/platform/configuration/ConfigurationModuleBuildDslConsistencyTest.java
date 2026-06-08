package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the configuration module uses Kotlin DSL consistently and follows
 * Gradle build-file conventions. The Spring Boot plugin's Kotlin DSL API
 * (type-safe accessors, extension functions) differs from Groovy DSL; mixing
 * DSL styles or using incorrect file extensions would cause compilation failures
 * or subtle behavior differences (e.g., Groovy's dynamic method resolution vs
 * Kotlin's static typing).
 * <p>
 * Also guards against the presence of Groovy build files that could conflict
 * with the Kotlin DSL build file (Gradle picks one; having both is an error).
 */
class ConfigurationModuleBuildDslConsistencyTest {

    private static Path projectRoot;
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
        projectRoot = dir;
        buildContent = Files.readString(dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void kotlinDslBuildFile_shouldExist() {
        assertThat(Files.exists(projectRoot.resolve("configuration/build.gradle.kts")))
                .as("configuration module must use build.gradle.kts (Kotlin DSL) — "
                        + "the version catalog alias() syntax requires Kotlin DSL for type-safe accessors")
                .isTrue();
    }

    @Test
    void groovyDslBuildFile_shouldNotExist() {
        assertThat(Files.exists(projectRoot.resolve("configuration/build.gradle")))
                .as("configuration module must NOT have a Groovy build.gradle file — "
                        + "having both build.gradle and build.gradle.kts causes a Gradle error "
                        + "and the Kotlin DSL is the standard for this project")
                .isFalse();
    }

    @Test
    void settingsFile_shouldUseKotlinDsl() {
        assertThat(Files.exists(projectRoot.resolve("settings.gradle.kts")))
                .as("Project must use settings.gradle.kts (Kotlin DSL) for consistency "
                        + "with module build files and version catalog type-safe accessors")
                .isTrue();
    }

    @Test
    void settingsFile_shouldNotHaveGroovyEquivalent() {
        assertThat(Files.exists(projectRoot.resolve("settings.gradle")))
                .as("Project must NOT have a Groovy settings.gradle alongside settings.gradle.kts")
                .isFalse();
    }

    @Test
    void buildFile_shouldNotUseGroovyStringInterpolation() {
        assertThat(buildContent)
                .as("build.gradle.kts must NOT use Groovy-style string interpolation ($variable) "
                        + "outside of Kotlin string templates — this could indicate a copy-paste "
                        + "from Groovy DSL documentation")
                .doesNotContainPattern("\\$\\{?[a-z].*\\}?(?!.*\\.get\\(\\))");
    }

    @Test
    void buildFile_shouldNotUseGroovySingleQuotes() {
        assertThat(buildContent)
                .as("build.gradle.kts must NOT use single quotes for strings — "
                        + "Kotlin uses double quotes; single quotes are Groovy char literals")
                .doesNotContain("'");
    }

    @Test
    void buildFile_shouldNotUseDynamicMethodCalls() {
        assertThat(buildContent)
                .as("build.gradle.kts must NOT use def or untyped dynamic calls — "
                        + "these are Groovy constructs not valid in Kotlin DSL")
                .doesNotContainPattern("\\bdef\\s+")
                .doesNotContainPattern("\\bvar\\s+\\w+\\s*$");
    }
}
