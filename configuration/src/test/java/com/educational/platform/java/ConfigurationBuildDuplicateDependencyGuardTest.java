package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against duplicate dependency declarations in configuration/build.gradle.kts.
 * <p>
 * The Java 26 upgrade added three new test dependencies (junit-jupiter-params,
 * junit-jupiter-engine, assertj-core) alongside existing declarations. This test
 * ensures no dependency artifact appears more than once, which would cause
 * Gradle resolution ambiguity, classpath duplication, and potentially conflicting
 * versions at runtime.
 * <p>
 * {@link BuildDependencyAlignmentTest} validates dependency presence and scope.
 * {@link TestDependencyScopeValidationTest} validates correct scope usage.
 * This test specifically validates <em>uniqueness</em> of each artifact declaration.
 */
public class ConfigurationBuildDuplicateDependencyGuardTest {

    private static final Pattern DEPENDENCY_ARTIFACT = Pattern.compile(
            "(?:testImplementation|testRuntimeOnly|implementation)\\s*\\(.*?\"([^\"]+)\".*?\\)");

    @Test
    void configBuild_shouldNotHave_duplicateTestDependencies() throws IOException {
        String content = readConfigBuildGradle();
        List<String> lines = content.lines().toList();

        List<String> depArtifacts = lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("testImplementation") || line.startsWith("testRuntimeOnly"))
                .filter(line -> !line.startsWith("//"))
                .map(this::extractArtifactId)
                .filter(id -> !id.isEmpty())
                .toList();

        Map<String, Long> counts = depArtifacts.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            assertThat(entry.getValue())
                    .as("Test dependency '%s' should be declared exactly once", entry.getKey())
                    .isEqualTo(1L);
        }
    }

    @Test
    void configBuild_shouldNotHave_duplicateImplementationDependencies() throws IOException {
        String content = readConfigBuildGradle();
        List<String> lines = content.lines().toList();

        List<String> depArtifacts = lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("implementation("))
                .filter(line -> !line.startsWith("//"))
                .map(this::extractArtifactId)
                .filter(id -> !id.isEmpty())
                .toList();

        Map<String, Long> counts = depArtifacts.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            assertThat(entry.getValue())
                    .as("Implementation dependency '%s' should be declared exactly once", entry.getKey())
                    .isEqualTo(1L);
        }
    }

    @Test
    void configBuild_shouldNotDeclare_sameArtifact_inMultipleScopes() throws IOException {
        String content = readConfigBuildGradle();
        List<String> lines = content.lines().toList();

        List<String> testImplArtifacts = extractArtifactsForScope(lines, "testImplementation");
        List<String> testRuntimeArtifacts = extractArtifactsForScope(lines, "testRuntimeOnly");

        for (String artifact : testRuntimeArtifacts) {
            assertThat(testImplArtifacts)
                    .as("Artifact '%s' declared as testRuntimeOnly should not also be testImplementation", artifact)
                    .doesNotContain(artifact);
        }
    }

    @Test
    void configBuild_testDependencyCount_shouldMatch_expectedNumber() throws IOException {
        String content = readConfigBuildGradle();
        List<String> lines = content.lines().toList();

        long testDepCount = lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("testImplementation") || line.startsWith("testRuntimeOnly"))
                .filter(line -> !line.startsWith("//"))
                .count();

        // Expected: junit-jupiter-api, junit-jupiter-params, junit-jupiter-engine,
        //           junit-platform-engine, junit-platform-launcher,
        //           mockito-junit-jupiter, assertj-core, archunit-junit5
        assertThat(testDepCount)
                .as("Configuration module should have exactly 8 test dependency declarations")
                .isEqualTo(8);
    }

    private List<String> extractArtifactsForScope(List<String> lines, String scope) {
        return lines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith(scope))
                .filter(line -> !line.startsWith("//"))
                .map(this::extractArtifactId)
                .filter(id -> !id.isEmpty())
                .toList();
    }

    private String extractArtifactId(String line) {
        // Match artifact patterns like "junit-jupiter-api" or project(":foo")
        Matcher quotedArtifact = Pattern.compile("\"([a-zA-Z][a-zA-Z0-9._-]+)\"").matcher(line);
        if (quotedArtifact.find()) {
            String firstMatch = quotedArtifact.group(1);
            // Skip group IDs (org.junit.jupiter, etc.) — take the second quoted string if available
            if (quotedArtifact.find()) {
                return quotedArtifact.group(1);
            }
            return firstMatch;
        }
        // Handle project() references
        Matcher projectRef = Pattern.compile("project\\(\"([^\"]+)\"\\)").matcher(line);
        if (projectRef.find()) {
            return projectRef.group(1);
        }
        return "";
    }

    private String readConfigBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
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
