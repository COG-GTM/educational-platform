package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the top-level structural ordering and completeness of
 * configuration/build.gradle.kts after the PR added the plugins block
 * and spring-boot-starter-test dependency. Gradle Kotlin DSL requires
 * the plugins block to appear at the very start of the file (before any
 * other statement); violating this causes a build script compilation error.
 * These tests guard against structural regressions that would break the build.
 */
class ConfigurationBuildFileStructuralOrderTest {

    private static String buildContent;
    private static List<String> buildLines;

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
        buildLines = buildContent.lines().toList();
    }

    @Test
    void pluginsBlock_shouldStartAtFirstLine() {
        String firstNonEmpty = buildLines.stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .orElse("");
        assertThat(firstNonEmpty)
                .as("The plugins block must be the very first statement in build.gradle.kts — "
                        + "Gradle Kotlin DSL requires plugins {} before any other construct")
                .startsWith("plugins");
    }

    @Test
    void buildFile_shouldContainExactlyThreeTopLevelBlocks() {
        // Top-level blocks are lines that start at column 0 with a keyword followed by '{'
        List<String> topLevelBlocks = buildLines.stream()
                .filter(line -> !line.isEmpty() && !Character.isWhitespace(line.charAt(0)))
                .filter(line -> line.contains("{"))
                .map(line -> line.split("\\s*\\{")[0].trim())
                .toList();
        assertThat(topLevelBlocks)
                .as("build.gradle.kts must contain exactly three top-level blocks: "
                        + "plugins, dependencies, tasks.test — additional blocks could "
                        + "indicate accidental configuration that conflicts with the Spring Boot plugin defaults")
                .containsExactly("plugins", "dependencies", "tasks.test");
    }

    @Test
    void dependenciesBlock_shouldFollowPluginsBlock() {
        int pluginsEnd = buildContent.indexOf("}", buildContent.indexOf("plugins"));
        int dependenciesStart = buildContent.indexOf("dependencies");
        assertThat(dependenciesStart)
                .as("dependencies block must follow the plugins block")
                .isGreaterThan(pluginsEnd);
    }

    @Test
    void tasksTestBlock_shouldFollowDependenciesBlock() {
        int dependenciesEnd = buildContent.lastIndexOf("}",
                buildContent.indexOf("tasks.test"));
        int tasksTestStart = buildContent.indexOf("tasks.test");
        assertThat(tasksTestStart)
                .as("tasks.test block must follow the dependencies block")
                .isGreaterThan(dependenciesEnd);
    }

    @Test
    void buildFile_shouldNotContainBuildscriptBlock() {
        assertThat(buildContent)
                .as("build.gradle.kts must NOT contain a buildscript block — "
                        + "the plugins DSL with version catalog replaces the legacy buildscript approach")
                .doesNotContainPattern("^buildscript\\s*\\{");
    }

    @Test
    void buildFile_shouldNotContainAllprojectsBlock() {
        assertThat(buildContent)
                .as("configuration module build must NOT contain an allprojects block — "
                        + "cross-module configuration belongs in the root build.gradle.kts")
                .doesNotContain("allprojects");
    }

    @Test
    void buildFile_shouldNotContainSubprojectsBlock() {
        assertThat(buildContent)
                .as("configuration module build must NOT contain a subprojects block — "
                        + "the configuration module is a leaf module, not a parent")
                .doesNotContain("subprojects");
    }

    @Test
    void buildFile_shouldNotContainRepositoriesBlock() {
        assertThat(buildContent)
                .as("configuration module build should NOT declare repositories — "
                        + "repositories are configured at the root level for consistency")
                .doesNotContainPattern("^repositories\\s*\\{");
    }
}
