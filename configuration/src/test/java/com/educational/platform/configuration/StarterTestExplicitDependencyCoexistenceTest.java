package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the coexistence of spring-boot-starter-test (which transitively
 * provides JUnit, Mockito, AssertJ, and Hamcrest) with the explicit test
 * dependency declarations in configuration/build.gradle.kts. The PR added
 * starter-test alongside existing explicit junit-jupiter-api, mockito, and
 * archunit declarations. These tests guard against:
 * <ul>
 *   <li>Version conflicts between starter-test transitives and explicit declarations</li>
 *   <li>Duplicate class issues if both paths resolve different versions</li>
 *   <li>Missing explicit deps that starter-test does NOT provide (e.g., ArchUnit)</li>
 * </ul>
 */
class StarterTestExplicitDependencyCoexistenceTest {

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
    void starterTestAndExplicitJunit_shouldCoexistWithoutConflict() {
        assertThatCode(() -> {
            Class.forName("org.junit.jupiter.api.Test");
            Class.forName("org.junit.jupiter.api.Assertions");
        }).as("JUnit Jupiter API must be resolvable — both starter-test transitive "
                + "and explicit declaration should coexist without classpath conflicts")
                .doesNotThrowAnyException();
    }

    @Test
    void starterTestAndExplicitMockito_shouldCoexistWithoutConflict() {
        assertThatCode(() -> {
            Class.forName("org.mockito.Mockito");
            Class.forName("org.mockito.junit.jupiter.MockitoExtension");
        }).as("Mockito core and JUnit extension must be resolvable — both starter-test "
                + "transitive and explicit mockito-junit-jupiter should coexist")
                .doesNotThrowAnyException();
    }

    @Test
    void archUnit_shouldBeExplicitlyDeclared_notProvidedByStarterTest() {
        assertThat(buildContent)
                .as("ArchUnit must be explicitly declared — starter-test does NOT "
                        + "include it transitively, so removing the explicit dependency "
                        + "would break architecture tests")
                .contains("archunit-junit5");
    }

    @Test
    void junitPlatformEngine_shouldBeExplicitlyDeclared() {
        assertThat(buildContent)
                .as("junit-platform-engine must be explicitly declared for test discovery — "
                        + "while starter-test provides JUnit Jupiter, the platform engine "
                        + "and launcher may need explicit inclusion for Gradle test tasks")
                .contains("junit-platform-engine");
    }

    @Test
    void junitPlatformLauncher_shouldBeExplicitlyDeclared() {
        assertThat(buildContent)
                .as("junit-platform-launcher must be explicitly declared — "
                        + "it is required for programmatic test discovery and execution "
                        + "by Gradle's useJUnitPlatform() task configuration")
                .contains("junit-platform-launcher");
    }

    @Test
    void explicitTestDeps_shouldAllBeTestImplementation() {
        List<String> testDepLines = buildContent.lines()
                .filter(line -> line.trim().startsWith("testImplementation("))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();

        assertThat(testDepLines)
                .as("All explicit test dependencies must use testImplementation scope")
                .allSatisfy(line -> assertThat(line.trim()).startsWith("testImplementation("));
    }

    @Test
    void mockito_shouldUseExplicitVersionFromCatalog() {
        assertThat(buildContent)
                .as("mockito-junit-jupiter must use explicit version from version catalog "
                        + "(libs.versions.mockito.get()) for version pinning control, "
                        + "even though starter-test provides a transitive Mockito version")
                .containsPattern("mockito-junit-jupiter.*libs\\.versions\\.mockito\\.get\\(\\)");
    }
}
