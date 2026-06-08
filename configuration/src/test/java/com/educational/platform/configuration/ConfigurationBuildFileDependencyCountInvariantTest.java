package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the exact number of dependencies in configuration/build.gradle.kts
 * to catch accidental additions or removals. The configuration module is
 * the composition root — every dependency directly affects what the Spring
 * Boot plugin packages into the fat JAR (implementation) or what tests can
 * exercise (testImplementation). Unguarded additions can bloat the artifact,
 * slow startup, or introduce classpath conflicts that break bootRun.
 * <p>
 * Complements {@link BuildFileStarterTestDeclarationUniquenessTest} (no
 * duplicates) and {@link ConfigurationModuleDependencyScopeExclusivenessTest}
 * (scope correctness). This test guards the total count so that any new
 * dependency triggers a conscious test update.
 */
class ConfigurationBuildFileDependencyCountInvariantTest {

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
    void implementationDependencies_shouldHaveExactCount() {
        long count = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("implementation("))
                .count();
        assertThat(count)
                .as("configuration module must have exactly 20 implementation dependencies: "
                        + "18 project modules + spring-boot-starter-web + liquibase-core. "
                        + "Adding implementation deps changes the bootJar artifact — "
                        + "update this count deliberately, not accidentally.")
                .isEqualTo(20);
    }

    @Test
    void testImplementationDependencies_shouldHaveExactCount() {
        long count = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("testImplementation("))
                .count();
        assertThat(count)
                .as("configuration module must have exactly 6 testImplementation dependencies: "
                        + "spring-boot-starter-test, junit-jupiter-api, junit-platform-engine, "
                        + "junit-platform-launcher, mockito-junit-jupiter, archunit-junit5. "
                        + "Adding test deps may introduce version conflicts with starter-test "
                        + "transitives — update this count deliberately.")
                .isEqualTo(6);
    }

    @Test
    void totalDependencyDeclarations_shouldMatchExpected() {
        long total = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("implementation(") || line.startsWith("testImplementation("))
                .count();
        assertThat(total)
                .as("Total dependency count (implementation + testImplementation) must be 26")
                .isEqualTo(26);
    }

    @Test
    void projectModuleDependencies_shouldHaveExactCount() {
        long count = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("implementation(project("))
                .count();
        assertThat(count)
                .as("configuration module must depend on exactly 18 project modules: "
                        + "3 per bounded context (application, web, integration-events) × 5 contexts "
                        + "= 15 + security-config + web + common = 18")
                .isEqualTo(18);
    }

    @Test
    void externalImplementationDependencies_shouldHaveExactCount() {
        long count = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("implementation(\""))
                .count();
        assertThat(count)
                .as("configuration module must have exactly 2 external implementation deps: "
                        + "spring-boot-starter-web and liquibase-core")
                .isEqualTo(2);
    }
}
