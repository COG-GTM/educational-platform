package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@code tasks.test} block in configuration/build.gradle.kts
 * contains only the {@code useJUnitPlatform()} call and no additional test task
 * configuration. The Spring Boot plugin and spring-boot-starter-test provide
 * sensible defaults for test execution; adding custom configuration (e.g.,
 * {@code maxParallelForks}, {@code jvmArgs}, {@code systemProperty}) to the
 * {@code tasks.test} block could interfere with Spring Boot's test context
 * caching, lifecycle management, or the JUnit Platform's parallel execution
 * strategy.
 * <p>
 * Complements {@link BuildGradleTestFrameworkConfigurationTest} (which validates
 * that {@code useJUnitPlatform()} is present) and
 * {@link ConfigurationBuildFileStructuralOrderTest} (which validates the
 * top-level block ordering). This test guards the block's content minimality.
 */
class BuildFileTasksTestBlockMinimalityTest {

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
    void tasksTestBlock_shouldContainOnlyUseJUnitPlatform() {
        int blockStart = -1;
        int blockEnd = -1;
        for (int i = 0; i < buildLines.size(); i++) {
            if (buildLines.get(i).trim().startsWith("tasks.test")) {
                blockStart = i;
            }
            if (blockStart >= 0 && blockEnd < 0 && buildLines.get(i).trim().equals("}")) {
                blockEnd = i;
            }
        }
        assertThat(blockStart).as("tasks.test block must exist").isGreaterThanOrEqualTo(0);
        assertThat(blockEnd).as("tasks.test block must have closing brace").isGreaterThan(blockStart);

        List<String> blockStatements = buildLines.subList(blockStart + 1, blockEnd).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        assertThat(blockStatements)
                .as("tasks.test block must contain exactly one statement: useJUnitPlatform()")
                .hasSize(1)
                .first()
                .asString()
                .isEqualTo("useJUnitPlatform()");
    }

    @Test
    void tasksTestBlock_shouldNotConfigureMaxParallelForks() {
        assertThat(buildContent)
                .as("tasks.test must NOT set maxParallelForks — "
                        + "Spring Boot test context caching relies on sequential class execution; "
                        + "parallel forks create separate JVM processes that defeat context caching")
                .doesNotContain("maxParallelForks");
    }

    @Test
    void tasksTestBlock_shouldNotConfigureJvmArgs() {
        assertThat(buildContent)
                .as("tasks.test must NOT set jvmArgs — "
                        + "JVM arguments for tests should be passed via gradle.properties "
                        + "or command line, not hardcoded in the build file")
                .doesNotContainPattern("tasks\\.test[^}]*jvmArgs");
    }

    @Test
    void tasksTestBlock_shouldNotConfigureSystemProperties() {
        assertThat(buildContent)
                .as("tasks.test must NOT set systemProperty — "
                        + "test-specific properties should use @SpringBootTest(properties) "
                        + "or @TestPropertySource for proper Spring property source precedence")
                .doesNotContainPattern("tasks\\.test[^}]*systemProperty");
    }

    @Test
    void tasksTestBlock_shouldNotConfigureFailFast() {
        assertThat(buildContent)
                .as("tasks.test must NOT set failFast — "
                        + "all tests should run to completion to surface all failures, "
                        + "not just the first one")
                .doesNotContain("failFast");
    }

    @Test
    void tasksTestBlock_shouldNotConfigureTestLogging() {
        assertThat(buildContent)
                .as("tasks.test must NOT configure testLogging — "
                        + "default logging is sufficient; verbose test logging "
                        + "should be activated via command-line flags (--info, --debug)")
                .doesNotContain("testLogging");
    }
}
