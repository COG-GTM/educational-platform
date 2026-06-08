package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the internal structure of the dependencies block in
 * configuration/build.gradle.kts. The PR added spring-boot-starter-test
 * as the first testImplementation dependency, before the existing JUnit
 * and Mockito entries. These tests guard against:
 * <ul>
 *   <li>Scope interleaving (testImplementation mixed with implementation lines)</li>
 *   <li>Unexpected total dependency count drift</li>
 *   <li>Incorrect ordering of starter-test relative to other test dependencies</li>
 * </ul>
 */
class BuildFileDependencyBlockStructureTest {

    private static String buildContent;
    private static List<String> dependencyLines;

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

        dependencyLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("//") && !line.startsWith("}")
                        && !line.equals("dependencies {") && !line.equals("plugins {")
                        && !line.equals("tasks.test {"))
                .filter(line -> line.startsWith("implementation(") || line.startsWith("testImplementation("))
                .toList();
    }

    @Test
    void implementationDeps_shouldAppearBeforeTestImplementationDeps() {
        int lastImplIdx = -1;
        int firstTestImplIdx = Integer.MAX_VALUE;

        for (int i = 0; i < dependencyLines.size(); i++) {
            if (dependencyLines.get(i).startsWith("implementation(")) {
                lastImplIdx = i;
            }
            if (dependencyLines.get(i).startsWith("testImplementation(") && i < firstTestImplIdx) {
                firstTestImplIdx = i;
            }
        }

        assertThat(lastImplIdx)
                .as("All implementation dependencies must appear before testImplementation "
                        + "dependencies — mixing scopes reduces readability")
                .isLessThan(firstTestImplIdx);
    }

    @Test
    void starterTest_shouldBeFirstTestDependency() {
        String firstTestDep = dependencyLines.stream()
                .filter(line -> line.startsWith("testImplementation("))
                .findFirst()
                .orElse("");
        assertThat(firstTestDep)
                .as("spring-boot-starter-test should be the first testImplementation dependency "
                        + "as it provides the foundational test infrastructure")
                .contains("spring-boot-starter-test");
    }

    @Test
    void dependencyBlock_shouldHaveExpectedImplementationCount() {
        long implCount = dependencyLines.stream()
                .filter(line -> line.startsWith("implementation("))
                .count();
        assertThat(implCount)
                .as("configuration module must have exactly 20 implementation dependencies "
                        + "(18 project modules + starter-web + liquibase)")
                .isEqualTo(20);
    }

    @Test
    void dependencyBlock_shouldHaveExpectedTestImplementationCount() {
        long testImplCount = dependencyLines.stream()
                .filter(line -> line.startsWith("testImplementation("))
                .count();
        assertThat(testImplCount)
                .as("configuration module must have exactly 5 testImplementation dependencies "
                        + "(starter-test, junit-jupiter-api, junit-platform-engine, "
                        + "junit-platform-launcher, mockito-junit-jupiter, archunit-junit5)")
                .isEqualTo(6);
    }

    @Test
    void dependencyBlock_shouldNotInterleaveScopeDeclarations() {
        boolean seenTestImpl = false;
        for (String line : dependencyLines) {
            if (line.startsWith("testImplementation(")) {
                seenTestImpl = true;
            } else if (line.startsWith("implementation(") && seenTestImpl) {
                assertThat(false)
                        .as("implementation dependency '%s' appears after testImplementation — "
                                + "all implementation deps must be grouped before testImplementation deps",
                                line)
                        .isTrue();
            }
        }
    }
}
