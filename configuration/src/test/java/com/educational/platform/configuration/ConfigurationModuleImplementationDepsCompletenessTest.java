package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the configuration module's implementation dependencies are
 * complete and correctly structured as the composition root of the modular
 * monolith. The Spring Boot plugin's bootRun/bootJar tasks package all
 * implementation dependencies into the executable artifact — a missing module
 * dependency would produce a ClassNotFoundException at runtime, while an
 * incorrect scope (e.g. testImplementation for a runtime module) would exclude
 * it from the bootJar.
 */
class ConfigurationModuleImplementationDepsCompletenessTest {

    private static String buildContent;
    private static List<String> implProjectLines;

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
        implProjectLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("implementation(project("))
                .toList();
    }

    @Test
    void allBoundedContexts_shouldHaveApplicationAndWebModules() {
        List<String> expectedModulePairs = List.of(
                ":users:users-application", ":users:users-web",
                ":courses:courses-application", ":courses:courses-web",
                ":administration:administration-application", ":administration:administration-web",
                ":course-enrollments:course-enrollments-application", ":course-enrollments:course-enrollments-web",
                ":course-reviews:course-reviews-application", ":course-reviews:course-reviews-web"
        );
        for (String module : expectedModulePairs) {
            assertThat(implProjectLines.stream().anyMatch(l -> l.contains(module)))
                    .as("Bounded context module '%s' must be an implementation dependency — "
                            + "bootRun/bootJar needs it on the runtime classpath", module)
                    .isTrue();
        }
    }

    @Test
    void allBoundedContexts_shouldHaveIntegrationEventsModule() {
        List<String> expectedEventModules = List.of(
                ":users:users-integration-events",
                ":courses:courses-integration-events",
                ":administration:administration-integration-events",
                ":course-enrollments:course-enrollments-integration-events",
                ":course-reviews:course-reviews-integration-events"
        );
        for (String module : expectedEventModules) {
            assertThat(implProjectLines.stream().anyMatch(l -> l.contains(module)))
                    .as("Integration events module '%s' must be an implementation dependency — "
                            + "async event publishing between bounded contexts requires it at runtime",
                            module)
                    .isTrue();
        }
    }

    @Test
    void securityConfig_shouldBeImplementationDependency() {
        assertThat(implProjectLines.stream().anyMatch(l -> l.contains(":security:security-config")))
                .as("security-config must be an implementation dependency — "
                        + "Spring Security auto-configuration requires it at runtime via bootRun")
                .isTrue();
    }

    @Test
    void webModule_shouldBeImplementationDependency() {
        assertThat(implProjectLines.stream().anyMatch(l -> l.contains("\":web\"")))
                .as("web module must be an implementation dependency — "
                        + "aggregated API documentation and web infrastructure are required at runtime")
                .isTrue();
    }

    @Test
    void commonModule_shouldBeImplementationDependency() {
        assertThat(implProjectLines.stream().anyMatch(l -> l.contains("\":common\"")))
                .as("common module must be an implementation dependency — "
                        + "shared exceptions and DTOs are required at runtime")
                .isTrue();
    }

    @Test
    void noModuleDependency_shouldUseTestScope() {
        List<String> testProjectDeps = buildContent.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("testImplementation(project("))
                .toList();
        assertThat(testProjectDeps)
                .as("No module should be declared as testImplementation in the configuration module — "
                        + "all modules are needed at runtime for bootRun/bootJar and must use implementation scope")
                .isEmpty();
    }
}
