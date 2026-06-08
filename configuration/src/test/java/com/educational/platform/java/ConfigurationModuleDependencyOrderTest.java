package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the dependency declaration ordering in configuration/build.gradle.kts.
 * <p>
 * The Java 26 upgrade added three new test dependencies
 * (junit-jupiter-params, junit-jupiter-engine, assertj-core).
 * This test verifies they are placed in the correct conventional order:
 * <ol>
 *   <li>{@code implementation} project dependencies (submodules)</li>
 *   <li>{@code implementation} external dependencies</li>
 *   <li>{@code testImplementation} dependencies</li>
 *   <li>{@code testRuntimeOnly} dependencies</li>
 * </ol>
 * <p>
 * Within the test scope, JUnit dependencies should appear before Mockito/AssertJ/ArchUnit.
 * This ordering convention is enforced to maintain readability and avoid accidental
 * dependency shadowing.
 */
public class ConfigurationModuleDependencyOrderTest {

    @Test
    void implementationDeps_shouldAppearBefore_testImplementation() throws IOException {
        String content = readConfigBuildGradle();
        String[] lines = content.split("\n");

        int lastImplementationLine = -1;
        int firstTestImplementationLine = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("implementation(") && !trimmed.startsWith("testImplementation(")) {
                lastImplementationLine = i;
            }
            if (trimmed.startsWith("testImplementation(")) {
                firstTestImplementationLine = Math.min(firstTestImplementationLine, i);
            }
        }

        assertThat(lastImplementationLine)
                .as("implementation dependencies should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(firstTestImplementationLine)
                .as("testImplementation dependencies should exist")
                .isNotEqualTo(Integer.MAX_VALUE);

        assertThat(lastImplementationLine)
                .as("All implementation deps should come before testImplementation deps")
                .isLessThan(firstTestImplementationLine);
    }

    @Test
    void testRuntimeOnly_shouldAppearAfter_testImplementation() throws IOException {
        String content = readConfigBuildGradle();
        String[] lines = content.split("\n");

        int firstTestRuntimeOnlyLine = Integer.MAX_VALUE;
        int firstTestImplementationLine = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("testRuntimeOnly(")) {
                firstTestRuntimeOnlyLine = Math.min(firstTestRuntimeOnlyLine, i);
            }
            if (trimmed.startsWith("testImplementation(")) {
                firstTestImplementationLine = Math.min(firstTestImplementationLine, i);
            }
        }

        assertThat(firstTestImplementationLine)
                .as("testImplementation should exist")
                .isNotEqualTo(Integer.MAX_VALUE);

        // testRuntimeOnly can be interleaved but first occurrence should not precede
        // first testImplementation
        if (firstTestRuntimeOnlyLine != Integer.MAX_VALUE) {
            assertThat(firstTestRuntimeOnlyLine)
                    .as("testRuntimeOnly should not appear before any testImplementation")
                    .isGreaterThan(firstTestImplementationLine);
        }
    }

    @Test
    void jupiterDeps_shouldAppearBefore_thirdPartyTestDeps() throws IOException {
        String content = readConfigBuildGradle();
        String[] lines = content.split("\n");

        int lastJupiterLine = -1;
        int firstThirdPartyLine = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.contains("junit.jupiter") || trimmed.contains("junit.platform")) {
                lastJupiterLine = i;
            }
            if (trimmed.contains("mockito") || trimmed.contains("assertj") || trimmed.contains("archunit")) {
                firstThirdPartyLine = Math.min(firstThirdPartyLine, i);
            }
        }

        assertThat(lastJupiterLine)
                .as("JUnit/Jupiter dependencies should exist")
                .isGreaterThanOrEqualTo(0);

        assertThat(firstThirdPartyLine)
                .as("Third-party test dependencies should exist")
                .isNotEqualTo(Integer.MAX_VALUE);

        assertThat(lastJupiterLine)
                .as("All JUnit/Jupiter deps should appear before Mockito/AssertJ/ArchUnit")
                .isLessThan(firstThirdPartyLine);
    }

    @ParameterizedTest(name = "Dependency ''{0}'' should use scope ''{1}''")
    @CsvSource({
            "spring-boot-starter-web, implementation",
            "liquibase-core,          implementation",
            "junit-jupiter-api,       testImplementation",
            "junit-jupiter-params,    testImplementation",
            "junit-jupiter-engine,    testRuntimeOnly",
            "junit-platform-engine,   testImplementation",
            "junit-platform-launcher, testImplementation",
            "mockito-junit-jupiter,   testImplementation",
            "assertj-core,            testImplementation",
            "archunit-junit5,         testImplementation"
    })
    void dependency_shouldUse_correctScope(String artifact, String expectedScope) throws IOException {
        String content = readConfigBuildGradle();
        String[] lines = content.split("\n");

        boolean found = false;
        for (String line : lines) {
            if (line.contains(artifact)) {
                found = true;
                assertThat(line.trim())
                        .as("Dependency '%s' should be declared with scope '%s'", artifact, expectedScope)
                        .startsWith(expectedScope + "(");
            }
        }

        assertThat(found)
                .as("Dependency '%s' should be declared in configuration/build.gradle.kts", artifact)
                .isTrue();
    }

    @Test
    void projectDeps_shouldAppearBefore_externalDeps() throws IOException {
        String content = readConfigBuildGradle();
        String[] lines = content.split("\n");

        int lastProjectDep = -1;
        int firstExternalDep = Integer.MAX_VALUE;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("implementation(project(")) {
                lastProjectDep = i;
            }
            if (trimmed.startsWith("implementation(\"org.")) {
                firstExternalDep = Math.min(firstExternalDep, i);
            }
        }

        if (lastProjectDep >= 0 && firstExternalDep != Integer.MAX_VALUE) {
            assertThat(lastProjectDep)
                    .as("Project dependencies should appear before external dependencies")
                    .isLessThan(firstExternalDep);
        }
    }

    @Test
    void noDuplicateDependencyDeclarations() throws IOException {
        String content = readConfigBuildGradle();
        String[] lines = content.split("\n");
        List<String> depLines = new ArrayList<>();

        // Match scope("group", "artifact"...) — include both group and artifact
        Pattern depPattern = Pattern.compile(
                "^\\s*(\\w+)\\(\"([^\"]+)\",\\s*\"([^\"]+)\"");
        for (String line : lines) {
            Matcher matcher = depPattern.matcher(line);
            if (matcher.find()) {
                depLines.add(matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3));
            }
        }

        assertThat(depLines)
                .as("There should be no duplicate dependency declarations (scope:group:artifact)")
                .doesNotHaveDuplicates();
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
