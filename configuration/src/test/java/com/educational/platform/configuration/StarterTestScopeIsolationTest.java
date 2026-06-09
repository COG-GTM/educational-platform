package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that spring-boot-starter-test is declared exclusively in the
 * {@code testImplementation} scope and does NOT leak into production
 * configurations (implementation, api, compileOnly, runtimeOnly). If
 * starter-test leaked to a production scope, Mockito, JUnit, and test
 * infrastructure classes would end up in the bootJar, increasing artifact
 * size and potentially causing ClassCastExceptions at runtime.
 * <p>
 * Also validates that no other module's build file declares starter-test
 * in a production scope, which could transitively pollute the configuration
 * module's production classpath via {@code project()} dependencies.
 * <p>
 * Complements {@link BuildFileNegativeScopeValidationTest} (which checks
 * specific scope exclusions) and {@link BuildDependencyScopeValidationTest}
 * (which validates general scope correctness).
 */
class StarterTestScopeIsolationTest {

    private static Path projectRoot;
    private static String configBuildContent;

    @BeforeAll
    static void loadBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        configBuildContent = Files.readString(
                dir.resolve("configuration/build.gradle.kts"));
    }

    @Test
    void starterTest_shouldNotBeInImplementationScope() {
        List<String> violations = configBuildContent.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> line.trim().startsWith("implementation("))
                .toList();
        assertThat(violations)
                .as("spring-boot-starter-test must NOT be in implementation scope — "
                        + "test infrastructure classes would end up in the production bootJar")
                .isEmpty();
    }

    @Test
    void starterTest_shouldNotBeInApiScope() {
        List<String> violations = configBuildContent.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> line.trim().startsWith("api("))
                .toList();
        assertThat(violations)
                .as("spring-boot-starter-test must NOT be in api scope — "
                        + "test dependencies must not be exposed to dependent modules")
                .isEmpty();
    }

    @Test
    void starterTest_shouldNotBeInRuntimeOnlyScope() {
        List<String> violations = configBuildContent.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> line.trim().startsWith("runtimeOnly("))
                .toList();
        assertThat(violations)
                .as("spring-boot-starter-test must NOT be in runtimeOnly scope — "
                        + "it must be available at test compile time for @SpringBootTest")
                .isEmpty();
    }

    @Test
    void starterTest_shouldNotBeInCompileOnlyScope() {
        List<String> violations = configBuildContent.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> line.trim().startsWith("compileOnly("))
                .toList();
        assertThat(violations)
                .as("spring-boot-starter-test must NOT be in compileOnly scope — "
                        + "test infrastructure must be available at both compile and runtime")
                .isEmpty();
    }

    @Test
    void starterTest_shouldBeInTestImplementationScope() {
        boolean inTestImpl = configBuildContent.lines()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter-test"))
                .anyMatch(line -> line.trim().startsWith("testImplementation("));
        assertThat(inTestImpl)
                .as("spring-boot-starter-test must be declared as testImplementation — "
                        + "this is the correct scope for test-only dependencies")
                .isTrue();
    }

    @Test
    void noOtherModule_shouldDeclareStarterTestInProductionScope() throws IOException {
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .filter(p -> !p.toString().contains(".gradle/"))
                    .filter(p -> !p.toString().contains("configuration/build.gradle.kts"))
                    .toList();

            for (Path buildFile : buildFiles) {
                String content = Files.readString(buildFile);
                List<String> violations = content.lines()
                        .filter(line -> !line.trim().startsWith("//"))
                        .filter(line -> line.contains("spring-boot-starter-test"))
                        .filter(line -> line.trim().startsWith("implementation(")
                                || line.trim().startsWith("api(")
                                || line.trim().startsWith("runtimeOnly("))
                        .toList();
                assertThat(violations)
                        .as("Module %s must NOT declare spring-boot-starter-test "
                                + "in a production scope — this would transitively pollute "
                                + "the configuration module's production classpath",
                                projectRoot.relativize(buildFile))
                        .isEmpty();
            }
        }
    }
}
