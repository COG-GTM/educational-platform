package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the configuration module uses only the expected dependency
 * scopes: {@code implementation} for runtime dependencies and
 * {@code testImplementation} for test dependencies. Using other scopes
 * would violate the modular monolith architecture:
 * <ul>
 *   <li>{@code api} — would export Spring Boot dependencies to consumer modules
 *       (no module should consume the configuration module as a dependency)</li>
 *   <li>{@code compileOnly} — would strip Spring Boot starters from the runtime
 *       classpath, breaking bootRun/bootJar</li>
 *   <li>{@code runtimeOnly} — would hide Spring Boot APIs from compile-time
 *       checks, defeating the purpose of type-safe configuration</li>
 * </ul>
 * Complements {@link BuildDependencyScopeValidationTest} which validates
 * individual dependency scopes, and {@link BuildFileNegativeScopeValidationTest}
 * which guards against specific scope misuses for starter-test/starter-web.
 */
class ConfigurationModuleDependencyScopeExclusivenessTest {

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
    void buildFile_shouldNotUseApiScope() {
        List<String> apiLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("api("))
                .toList();
        assertThat(apiLines)
                .as("configuration module must NOT use api() scope — "
                        + "it is the composition root and no other module should "
                        + "depend on it; api() would export Spring Boot dependencies")
                .isEmpty();
    }

    @Test
    void buildFile_shouldNotUseCompileOnlyScope() {
        List<String> compileOnlyLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("compileOnly("))
                .toList();
        assertThat(compileOnlyLines)
                .as("configuration module must NOT use compileOnly() scope — "
                        + "all dependencies are needed at runtime for bootRun/bootJar")
                .isEmpty();
    }

    @Test
    void buildFile_shouldNotUseRuntimeOnlyScope() {
        List<String> runtimeOnlyLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("runtimeOnly("))
                .toList();
        assertThat(runtimeOnlyLines)
                .as("configuration module must NOT use runtimeOnly() scope — "
                        + "Spring Boot starters need compile-time access for "
                        + "auto-configuration and annotation processing")
                .isEmpty();
    }

    @Test
    void buildFile_shouldNotUseTestCompileOnlyScope() {
        List<String> testCompileOnlyLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("testCompileOnly("))
                .toList();
        assertThat(testCompileOnlyLines)
                .as("configuration module must NOT use testCompileOnly() — "
                        + "test dependencies including starter-test need runtime access")
                .isEmpty();
    }

    @Test
    void implementationDeps_shouldNotContainTestArtifacts() {
        List<String> implLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//"))
                .filter(line -> line.startsWith("implementation(") && !line.startsWith("implementation(project("))
                .toList();
        for (String line : implLines) {
            assertThat(line)
                    .as("implementation scope must NOT contain test artifacts — "
                            + "test libraries belong in testImplementation")
                    .doesNotContain("starter-test")
                    .doesNotContain("junit")
                    .doesNotContain("mockito")
                    .doesNotContain("archunit");
        }
    }

    @Test
    void buildFile_shouldUseOnlyTwoScopeTypes() {
        List<String> allDepLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("//") && !line.isEmpty())
                .filter(line -> line.contains("(") && !line.startsWith("plugins")
                        && !line.startsWith("alias") && !line.startsWith("tasks")
                        && !line.startsWith("useJUnit") && !line.startsWith("}")
                        && !line.startsWith("{"))
                .filter(line -> line.startsWith("implementation(")
                        || line.startsWith("testImplementation(")
                        || line.startsWith("api(")
                        || line.startsWith("compileOnly(")
                        || line.startsWith("runtimeOnly(")
                        || line.startsWith("testRuntimeOnly(")
                        || line.startsWith("testCompileOnly(")
                        || line.startsWith("annotationProcessor(")
                        || line.startsWith("developmentOnly("))
                .toList();
        for (String line : allDepLines) {
            assertThat(line)
                    .as("All dependencies must use either implementation() or testImplementation() — "
                            + "found unexpected scope in: %s", line)
                    .satisfiesAnyOf(
                            l -> assertThat(l).startsWith("implementation("),
                            l -> assertThat(l).startsWith("testImplementation(")
                    );
        }
    }
}
