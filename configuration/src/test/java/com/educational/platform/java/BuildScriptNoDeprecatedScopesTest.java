package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against usage of deprecated Gradle dependency scopes in any
 * {@code build.gradle.kts} file across the project.
 * <p>
 * Gradle deprecated the {@code compile}, {@code runtime}, {@code testCompile},
 * and {@code testRuntime} configurations in Gradle 6 and removed them in
 * Gradle 7+. With the upgrade to Gradle 9.5.1, using any of these would
 * cause an immediate build failure.
 * <p>
 * This test scans <em>all</em> build files — not just the root and
 * configuration modules — to prevent any submodule from accidentally using
 * the old scope names (e.g. during a copy-paste from Stack Overflow or an
 * older project).
 * <p>
 * {@link ConfigurationModuleDependencyOrderTest} validates scope ordering.
 * {@link TestDependencyScopeValidationTest} validates specific dependency scopes.
 * This test validates that <em>no build file anywhere</em> uses legacy scopes.
 */
public class BuildScriptNoDeprecatedScopesTest {

    @ParameterizedTest(name = "No build file should use deprecated scope: {0}")
    @ValueSource(strings = {
            "compile(",
            "runtime(",
            "testCompile(",
            "testRuntime("
    })
    void noBuildFile_shouldUse_deprecatedScope(String deprecatedScope) throws IOException {
        List<Path> buildFiles = findAllBuildGradleFiles();

        assertThat(buildFiles)
                .as("Should find at least one build.gradle.kts file")
                .isNotEmpty();

        for (Path buildFile : buildFiles) {
            String content = Files.readString(buildFile);
            String[] lines = content.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                    continue;
                }
                assertThat(trimmed)
                        .as("Line %d in %s should not use deprecated scope '%s'",
                                i + 1, buildFile.getFileName(), deprecatedScope)
                        .doesNotStartWith(deprecatedScope);
            }
        }
    }

    @Test
    void allBuildFiles_shouldUse_onlyModernScopes() throws IOException {
        List<Path> buildFiles = findAllBuildGradleFiles();

        for (Path buildFile : buildFiles) {
            String content = Files.readString(buildFile);
            String[] lines = content.split("\n");

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("//") || trimmed.startsWith("/*")
                        || trimmed.startsWith("*") || trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.matches("^\\w+\\(.*")) {
                    String scope = trimmed.substring(0, trimmed.indexOf('('));
                    if (scope.equals("compile") || scope.equals("runtime")
                            || scope.equals("testCompile") || scope.equals("testRuntime")) {
                        assertThat(scope)
                                .as("Deprecated scope '%s' found in %s; use implementation/runtimeOnly/testImplementation/testRuntimeOnly",
                                        scope, buildFile.getFileName())
                                .isNotIn("compile", "runtime", "testCompile", "testRuntime");
                    }
                }
            }
        }
    }

    @Test
    void configurationBuildFile_shouldUse_onlyExpectedScopes() throws IOException {
        String content = Files.readString(
                findProjectRoot().resolve("configuration/build.gradle.kts"));
        String[] lines = content.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("{")
                    || trimmed.startsWith("}") || trimmed.startsWith("/*")
                    || trimmed.startsWith("*")) {
                continue;
            }
            if (trimmed.matches("^(implementation|testImplementation|testRuntimeOnly)\\(.*")) {
                continue;
            }
            if (trimmed.startsWith("useJUnitPlatform") || trimmed.startsWith("tasks.")
                    || trimmed.startsWith("dependencies")) {
                continue;
            }
            // Any remaining line with a parenthesized dependency call using an unexpected scope
            if (trimmed.matches("^(api|compileOnly|runtimeOnly|annotationProcessor)\\(.*")) {
                // These are valid modern scopes but not expected in configuration module
                assertThat(trimmed)
                        .as("Unexpected scope in configuration/build.gradle.kts: %s", trimmed)
                        .satisfiesAnyOf(
                                t -> assertThat(t).startsWith("implementation("),
                                t -> assertThat(t).startsWith("testImplementation("),
                                t -> assertThat(t).startsWith("testRuntimeOnly(")
                        );
            }
        }
    }

    @Test
    void rootBuildFile_shouldNotDeclare_directDependencies_withDeprecatedScopes() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));

        assertThat(content)
                .as("Root build.gradle.kts should not use deprecated compile scope")
                .doesNotContainPattern("(?m)^\\s*compile\\(")
                .doesNotContainPattern("(?m)^\\s*runtime\\(")
                .doesNotContainPattern("(?m)^\\s*testCompile\\(")
                .doesNotContainPattern("(?m)^\\s*testRuntime\\(");
    }

    private List<Path> findAllBuildGradleFiles() throws IOException {
        Path root = findProjectRoot();
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .filter(p -> !p.toString().contains(".gradle/"))
                    .toList();
        }
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path settingsFile = current.resolve("settings.gradle.kts");
            Path gradleDir = current.resolve("gradle/wrapper");
            if (Files.exists(settingsFile) || Files.isDirectory(gradleDir)) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
