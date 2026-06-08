package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Gradle actually resolves the spring-boot-starter-test dependency
 * into the configuration module's test classpath. Existing tests validate build
 * file syntax (BuildFileDependencyDeclarationConsistencyTest) and class-loading
 * (StarterTestTransitiveDependencyPresenceTest); this test validates the Gradle
 * dependency resolution engine itself by running {@code ./gradlew :configuration:dependencies}
 * and inspecting the output. Catches issues where the build file is syntactically
 * correct but Gradle's resolution strategy excludes or overrides the dependency
 * (e.g., conflict resolution, forced versions, or repository misconfiguration).
 */
class GradleDependencyResolutionVerificationTest {

    private static Path projectRoot;
    private static List<String> dependencyOutput;

    @BeforeAll
    static void runGradleDependencies() throws IOException, InterruptedException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;

        String wrapper = Files.exists(projectRoot.resolve("gradlew"))
                ? projectRoot.resolve("gradlew").toAbsolutePath().toString()
                : "gradle";

        ProcessBuilder pb = new ProcessBuilder(
                wrapper, ":configuration:dependencies",
                "--configuration", "testCompileClasspath", "-q")
                .directory(projectRoot.toFile())
                .redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            dependencyOutput = reader.lines().collect(Collectors.toList());
        }
        boolean finished = process.waitFor(180, TimeUnit.SECONDS);
        assertThat(finished)
                .as("Gradle dependencies command must complete within 180 seconds")
                .isTrue();
        assertThat(process.exitValue())
                .as("Gradle dependencies command must exit successfully (exit code 0)")
                .isEqualTo(0);
    }

    @Test
    void testCompileClasspath_shouldContainSpringBootStarterTest() {
        boolean hasStarterTest = dependencyOutput.stream()
                .anyMatch(line -> line.contains("spring-boot-starter-test"));
        assertThat(hasStarterTest)
                .as("testCompileClasspath must resolve spring-boot-starter-test — "
                        + "if missing, the dependency declaration in configuration/build.gradle.kts "
                        + "is not being resolved correctly by Gradle")
                .isTrue();
    }

    @Test
    void testCompileClasspath_shouldContainJunitJupiterApi() {
        boolean hasJunit = dependencyOutput.stream()
                .anyMatch(line -> line.contains("junit-jupiter-api"));
        assertThat(hasJunit)
                .as("testCompileClasspath must include junit-jupiter-api — "
                        + "it is both declared explicitly and brought transitively by starter-test")
                .isTrue();
    }

    @Test
    void testCompileClasspath_shouldContainMockitoCore() {
        boolean hasMockito = dependencyOutput.stream()
                .anyMatch(line -> line.contains("mockito"));
        assertThat(hasMockito)
                .as("testCompileClasspath must include mockito — "
                        + "required for @MockitoBean and mocking in tests")
                .isTrue();
    }

    @Test
    void testCompileClasspath_shouldContainSpringTest() {
        boolean hasSpringTest = dependencyOutput.stream()
                .anyMatch(line -> line.contains("spring-test"));
        assertThat(hasSpringTest)
                .as("testCompileClasspath must include spring-test — "
                        + "transitive from spring-boot-starter-test, provides TestContext framework")
                .isTrue();
    }

    @Test
    void testCompileClasspath_shouldContainSpringBootTest() {
        boolean hasSpringBootTest = dependencyOutput.stream()
                .anyMatch(line -> line.contains("spring-boot-test"));
        assertThat(hasSpringBootTest)
                .as("testCompileClasspath must include spring-boot-test — "
                        + "transitive from spring-boot-starter-test, provides @SpringBootTest")
                .isTrue();
    }

    @Test
    void testCompileClasspath_shouldNotContainSpringBootDevtools() {
        boolean hasDevtools = dependencyOutput.stream()
                .anyMatch(line -> line.contains("spring-boot-devtools"));
        assertThat(hasDevtools)
                .as("testCompileClasspath must NOT include spring-boot-devtools — "
                        + "devtools causes class-loading issues in tests and is not a "
                        + "transitive of spring-boot-starter-test")
                .isFalse();
    }
}
