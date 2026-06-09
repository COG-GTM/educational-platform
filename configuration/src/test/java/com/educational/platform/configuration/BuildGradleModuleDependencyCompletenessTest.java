package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that configuration/build.gradle.kts declares all required module
 * dependencies for the modular monolith to function correctly via bootRun.
 * The configuration module is the composition root — if any bounded-context
 * module dependency is accidentally removed, bootRun would fail with
 * ClassNotFoundException or NoSuchBeanDefinitionException at runtime.
 */
class BuildGradleModuleDependencyCompletenessTest {

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

    @ParameterizedTest
    @ValueSource(strings = {
            ":users:users-application",
            ":users:users-web",
            ":users:users-integration-events",
            ":administration:administration-application",
            ":administration:administration-web",
            ":administration:administration-integration-events",
            ":course-enrollments:course-enrollments-application",
            ":course-enrollments:course-enrollments-web",
            ":course-enrollments:course-enrollments-integration-events",
            ":course-reviews:course-reviews-application",
            ":course-reviews:course-reviews-web",
            ":course-reviews:course-reviews-integration-events",
            ":courses:courses-application",
            ":courses:courses-web",
            ":courses:courses-integration-events",
            ":security:security-config",
            ":web",
            ":common"
    })
    void buildFile_shouldDeclareModuleDependency(String modulePath) {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must depend on module '%s' — "
                                + "removing it would break bootRun with missing beans or classes",
                        modulePath)
                .contains("project(\"" + modulePath + "\")");
    }

    @Test
    void allModuleDependencies_shouldBeImplementationScope() {
        long moduleProjectLines = buildContent.lines()
                .filter(line -> line.contains("project(\""))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        long implementationModuleLines = buildContent.lines()
                .filter(line -> line.contains("project(\""))
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.trim().startsWith("implementation("))
                .count();
        assertThat(implementationModuleLines)
                .as("All module project dependencies must use implementation scope (not api, runtimeOnly, etc.)")
                .isEqualTo(moduleProjectLines);
    }

    @Test
    void buildFile_shouldDeclareExpectedNumberOfModuleDependencies() {
        long count = buildContent.lines()
                .filter(line -> line.contains("project(\""))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("configuration module must depend on exactly 18 sub-modules "
                        + "(5 bounded contexts x 3 + security + web + common)")
                .isEqualTo(18);
    }

    @Test
    void boundedContextModules_shouldHaveTriple_applicationWebEvents() {
        String[] contexts = {"users", "administration", "course-enrollments", "course-reviews", "courses"};
        for (String ctx : contexts) {
            assertThat(buildContent)
                    .as("Bounded context '%s' must have -application dependency", ctx)
                    .contains(ctx + "-application");
            assertThat(buildContent)
                    .as("Bounded context '%s' must have -web dependency", ctx)
                    .contains(ctx + "-web");
            assertThat(buildContent)
                    .as("Bounded context '%s' must have -integration-events dependency", ctx)
                    .contains(ctx + "-integration-events");
        }
    }
}
