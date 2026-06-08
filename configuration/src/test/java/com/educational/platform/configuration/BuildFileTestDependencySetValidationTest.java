package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the exact set and declaration ordering of test dependencies in
 * configuration/build.gradle.kts. The PR added spring-boot-starter-test as the
 * first test dependency, before explicit JUnit and Mockito declarations. These
 * tests guard against:
 * <ul>
 *   <li>Test dependency sprawl — adding unnecessary test libraries increases
 *       classpath complexity and build time</li>
 *   <li>Declaration ordering regression — starter-test should appear first
 *       because it brings managed versions of JUnit, Mockito, and AssertJ;
 *       explicit declarations that follow override only what is needed</li>
 *   <li>Missing test dependencies that would break test compilation</li>
 * </ul>
 */
class BuildFileTestDependencySetValidationTest {

    private static String buildContent;
    private static List<String> testDepLines;

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
        testDepLines = buildContent.lines()
                .filter(line -> line.trim().startsWith("testImplementation("))
                .filter(line -> !line.trim().startsWith("//"))
                .map(String::trim)
                .toList();
    }

    @Test
    void testDependencies_shouldHaveExactlyExpectedCount() {
        assertThat(testDepLines)
                .as("configuration/build.gradle.kts must have exactly 6 testImplementation "
                        + "declarations: starter-test, junit-jupiter-api, junit-platform-engine, "
                        + "junit-platform-launcher, mockito-junit-jupiter, archunit-junit5")
                .hasSize(6);
    }

    @Test
    void starterTest_shouldBeFirstTestDependency() {
        assertThat(testDepLines.getFirst())
                .as("spring-boot-starter-test must be the first testImplementation — "
                        + "it provides managed transitive versions that subsequent explicit "
                        + "declarations may need to override")
                .contains("spring-boot-starter-test");
    }

    @Test
    void junitJupiterApi_shouldFollowStarterTest() {
        int starterTestIdx = indexOf("spring-boot-starter-test");
        int jupiterIdx = indexOf("junit-jupiter-api");
        assertThat(jupiterIdx)
                .as("junit-jupiter-api must appear after spring-boot-starter-test")
                .isGreaterThan(starterTestIdx);
    }

    @Test
    void allExpectedTestDeps_shouldBePresent() {
        List<String> expectedArtifacts = List.of(
                "spring-boot-starter-test",
                "junit-jupiter-api",
                "junit-platform-engine",
                "junit-platform-launcher",
                "mockito-junit-jupiter",
                "archunit-junit5"
        );
        for (String artifact : expectedArtifacts) {
            assertThat(testDepLines.stream().anyMatch(line -> line.contains(artifact)))
                    .as("Test dependency '%s' must be declared in testImplementation", artifact)
                    .isTrue();
        }
    }

    @Test
    void testDependencies_shouldNotContainTestNg() {
        assertThat(testDepLines.stream().noneMatch(line -> line.contains("testng")))
                .as("TestNG must not be declared — the project uses JUnit 5 exclusively")
                .isTrue();
    }

    @Test
    void testDependencies_shouldNotContainSpock() {
        assertThat(testDepLines.stream().noneMatch(line -> line.contains("spock")))
                .as("Spock framework must not be declared — the project uses JUnit 5 exclusively")
                .isTrue();
    }

    @Test
    void testDependencies_shouldNotContainRestAssuredDirectly() {
        assertThat(testDepLines.stream().noneMatch(
                line -> line.contains("rest-assured") && !line.contains("io.rest-assured")))
                .as("REST-assured, if needed, should be declared via its full coordinates "
                        + "in the appropriate module, not in the configuration module")
                .isTrue();
    }

    @Test
    void buildFile_shouldNotUseTestRuntimeOnlyScope() {
        long testRuntimeOnlyCount = buildContent.lines()
                .filter(line -> line.trim().startsWith("testRuntimeOnly("))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(testRuntimeOnlyCount)
                .as("testRuntimeOnly should not be used — all test dependencies should be "
                        + "testImplementation for compile-time type safety")
                .isZero();
    }

    private int indexOf(String artifact) {
        for (int i = 0; i < testDepLines.size(); i++) {
            if (testDepLines.get(i).contains(artifact)) {
                return i;
            }
        }
        return -1;
    }
}
