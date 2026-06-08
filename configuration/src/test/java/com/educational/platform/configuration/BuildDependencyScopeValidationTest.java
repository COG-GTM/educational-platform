package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the dependency scope correctness in configuration/build.gradle.kts.
 * The PR added spring-boot-starter-test as testImplementation; these tests
 * guard against scope drift (e.g., accidentally changing to implementation)
 * and verify that runtime dependencies are not test-scoped.
 */
class BuildDependencyScopeValidationTest {

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
    void starterTest_shouldBeTestImplementation_notImplementation() {
        assertThat(buildContent)
                .as("spring-boot-starter-test must be testImplementation, not leaked into runtime")
                .containsPattern("testImplementation.*spring-boot-starter-test");
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .allMatch(line -> line.contains("testImplementation")))
                .as("All non-comment references to spring-boot-starter-test must use testImplementation")
                .isTrue();
    }

    @Test
    void starterWeb_shouldBeImplementation_notTestImplementation() {
        assertThat(buildContent)
                .as("spring-boot-starter-web must be implementation (runtime dependency)")
                .containsPattern("implementation.*spring-boot-starter-web");
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("testImplementation")))
                .as("spring-boot-starter-web must NOT be test-scoped")
                .isTrue();
    }

    @Test
    void junitDependencies_shouldBeTestScoped() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("junit"))
                .filter(line -> !line.trim().startsWith("//"))
                .allMatch(line -> line.contains("testImplementation")))
                .as("All JUnit dependencies must be test-scoped")
                .isTrue();
    }

    @Test
    void mockitoDependency_shouldBeTestScoped() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("mockito"))
                .filter(line -> !line.trim().startsWith("//"))
                .allMatch(line -> line.contains("testImplementation")))
                .as("Mockito dependency must be test-scoped")
                .isTrue();
    }

    @Test
    void archunitDependency_shouldBeTestScoped() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("archunit"))
                .filter(line -> !line.trim().startsWith("//"))
                .allMatch(line -> line.contains("testImplementation")))
                .as("ArchUnit dependency must be test-scoped")
                .isTrue();
    }

    @Test
    void liquibase_shouldBeImplementation_notTestScoped() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("liquibase"))
                .filter(line -> !line.trim().startsWith("//"))
                .allMatch(line -> line.contains("implementation(") || line.contains("implementation ")))
                .as("Liquibase must be a runtime dependency, not test-scoped")
                .isTrue();
    }

    @Test
    void starterTest_shouldNotBeDeclaredMultipleTimes() {
        long count = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("spring-boot-starter-test must be declared exactly once")
                .isEqualTo(1);
    }
}
