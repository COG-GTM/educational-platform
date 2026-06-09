package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the test framework configuration in configuration/build.gradle.kts.
 * The PR added spring-boot-starter-test; these tests guard the overall test
 * infrastructure setup: JUnit Platform activation, test dependency declarations,
 * and the absence of conflicting test configurations that would prevent
 * the new Spring Boot test infrastructure from functioning.
 */
class BuildGradleTestFrameworkConfigurationTest {

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
    void buildFile_shouldConfigureUseJUnitPlatform() {
        assertThat(buildContent)
                .as("tasks.test must call useJUnitPlatform() — required for JUnit 5 tests "
                        + "including @SpringBootTest from starter-test to be discovered and run")
                .contains("useJUnitPlatform()");
    }

    @Test
    void buildFile_shouldHaveTestBlock() {
        assertThat(buildContent)
                .as("build.gradle.kts must have a tasks.test configuration block")
                .containsPattern("tasks\\.test\\s*\\{");
    }

    @Test
    void buildFile_shouldNotUseTestNGRunner() {
        assertThat(buildContent)
                .as("TestNG runner must not be configured — project uses JUnit 5 exclusively")
                .doesNotContain("useTestNG");
    }

    @Test
    void buildFile_shouldNotExcludeJUnit5() {
        assertThat(buildContent)
                .as("JUnit 5 must not be excluded from the test classpath")
                .doesNotContainPattern("exclude.*junit-jupiter");
    }

    @Test
    void testDependencies_shouldIncludeAllRequiredFrameworks() {
        assertThat(buildContent)
                .as("spring-boot-starter-test must be present")
                .contains("spring-boot-starter-test");
        assertThat(buildContent)
                .as("junit-jupiter-api must be present")
                .contains("junit-jupiter-api");
        assertThat(buildContent)
                .as("mockito-junit-jupiter must be present")
                .contains("mockito-junit-jupiter");
        assertThat(buildContent)
                .as("archunit-junit5 must be present")
                .contains("archunit-junit5");
    }

    @Test
    void buildFile_shouldNotDisableTests() {
        assertThat(buildContent)
                .as("Test execution must not be disabled (enabled = false)")
                .doesNotContainPattern("tasks\\.test.*enabled\\s*=\\s*false");
    }

    @Test
    void buildFile_shouldNotFilterOutSpringBootTests() {
        assertThat(buildContent)
                .as("Build file should not filter/exclude SpringBoot test classes")
                .doesNotContainPattern("exclude.*SpringBoot")
                .doesNotContainPattern("exclude.*springboot");
    }
}
