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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that related test dependencies in configuration/build.gradle.kts
 * are grouped adjacently and follow the project's grouping convention.
 * <p>
 * The Java 26 upgrade added {@code junit-jupiter-params} (testImplementation),
 * {@code junit-jupiter-engine} (testRuntimeOnly), and {@code assertj-core}
 * (testImplementation). This test verifies:
 * <ul>
 *   <li>All JUnit Jupiter artifacts ({@code junit-jupiter-api},
 *       {@code junit-jupiter-params}, {@code junit-jupiter-engine}) appear
 *       adjacently — no unrelated dependency separates them</li>
 *   <li>JUnit Platform artifacts ({@code junit-platform-engine},
 *       {@code junit-platform-launcher}) appear as a contiguous group</li>
 *   <li>{@code junit-jupiter-engine} immediately follows the other Jupiter
 *       artifacts despite its different scope (testRuntimeOnly)</li>
 * </ul>
 * <p>
 * {@link ConfigurationModuleDependencyOrderTest} validates scope ordering
 * (implementation before testImplementation before testRuntimeOnly at the
 * group level). This test validates <em>artifact adjacency within groups</em>.
 */
public class ConfigurationBuildDependencyAdjacencyTest {

    @Test
    void jupiterArtifacts_shouldBe_contiguous() throws IOException {
        List<String> depLines = readDependencyLines();
        List<Integer> jupiterIndices = new ArrayList<>();

        for (int i = 0; i < depLines.size(); i++) {
            if (depLines.get(i).contains("junit-jupiter-")) {
                jupiterIndices.add(i);
            }
        }

        assertThat(jupiterIndices)
                .as("Should find at least 3 JUnit Jupiter artifacts (api, params, engine)")
                .hasSizeGreaterThanOrEqualTo(3);

        for (int i = 1; i < jupiterIndices.size(); i++) {
            assertThat(jupiterIndices.get(i) - jupiterIndices.get(i - 1))
                    .as("Jupiter artifacts at lines %d and %d should be adjacent (no gaps)",
                            jupiterIndices.get(i - 1), jupiterIndices.get(i))
                    .isEqualTo(1);
        }
    }

    @Test
    void jupiterParams_shouldAppear_immediatelyAfterJupiterApi() throws IOException {
        List<String> depLines = readDependencyLines();

        int apiIndex = -1;
        int paramsIndex = -1;

        for (int i = 0; i < depLines.size(); i++) {
            if (depLines.get(i).contains("junit-jupiter-api")) {
                apiIndex = i;
            }
            if (depLines.get(i).contains("junit-jupiter-params")) {
                paramsIndex = i;
            }
        }

        assertThat(apiIndex).as("junit-jupiter-api should exist").isGreaterThanOrEqualTo(0);
        assertThat(paramsIndex).as("junit-jupiter-params should exist").isGreaterThanOrEqualTo(0);

        assertThat(paramsIndex)
                .as("junit-jupiter-params should immediately follow junit-jupiter-api")
                .isEqualTo(apiIndex + 1);
    }

    @Test
    void jupiterEngine_shouldAppear_immediatelyAfterJupiterParams() throws IOException {
        List<String> depLines = readDependencyLines();

        int paramsIndex = -1;
        int engineIndex = -1;

        for (int i = 0; i < depLines.size(); i++) {
            if (depLines.get(i).contains("junit-jupiter-params")) {
                paramsIndex = i;
            }
            if (depLines.get(i).contains("junit-jupiter-engine")) {
                engineIndex = i;
            }
        }

        assertThat(paramsIndex).as("junit-jupiter-params should exist").isGreaterThanOrEqualTo(0);
        assertThat(engineIndex).as("junit-jupiter-engine should exist").isGreaterThanOrEqualTo(0);

        assertThat(engineIndex)
                .as("junit-jupiter-engine should immediately follow junit-jupiter-params")
                .isEqualTo(paramsIndex + 1);
    }

    @Test
    void platformArtifacts_shouldBe_contiguous() throws IOException {
        List<String> depLines = readDependencyLines();
        List<Integer> platformIndices = new ArrayList<>();

        for (int i = 0; i < depLines.size(); i++) {
            if (depLines.get(i).contains("junit-platform-")) {
                platformIndices.add(i);
            }
        }

        assertThat(platformIndices)
                .as("Should find at least 2 JUnit Platform artifacts (engine, launcher)")
                .hasSizeGreaterThanOrEqualTo(2);

        for (int i = 1; i < platformIndices.size(); i++) {
            assertThat(platformIndices.get(i) - platformIndices.get(i - 1))
                    .as("Platform artifacts at lines %d and %d should be adjacent",
                            platformIndices.get(i - 1), platformIndices.get(i))
                    .isEqualTo(1);
        }
    }

    @Test
    void jupiterGroup_shouldAppear_beforePlatformGroup() throws IOException {
        List<String> depLines = readDependencyLines();

        int lastJupiterIndex = -1;
        int firstPlatformIndex = Integer.MAX_VALUE;

        for (int i = 0; i < depLines.size(); i++) {
            if (depLines.get(i).contains("junit-jupiter-")) {
                lastJupiterIndex = i;
            }
            if (depLines.get(i).contains("junit-platform-")) {
                firstPlatformIndex = Math.min(firstPlatformIndex, i);
            }
        }

        assertThat(lastJupiterIndex).as("Jupiter artifacts should exist").isGreaterThanOrEqualTo(0);
        assertThat(firstPlatformIndex).as("Platform artifacts should exist").isNotEqualTo(Integer.MAX_VALUE);

        assertThat(lastJupiterIndex)
                .as("All Jupiter artifacts should appear before Platform artifacts")
                .isLessThan(firstPlatformIndex);
    }

    @ParameterizedTest(name = "Dependency ''{0}'' should appear exactly once in build file")
    @CsvSource({
            "junit-jupiter-api",
            "junit-jupiter-params",
            "junit-jupiter-engine",
            "junit-platform-engine",
            "junit-platform-launcher",
            "assertj-core",
            "archunit-junit5"
    })
    void testDependency_shouldAppear_exactlyOnce(String artifactId) throws IOException {
        List<String> depLines = readDependencyLines();

        long count = depLines.stream()
                .filter(line -> line.contains(artifactId))
                .count();

        assertThat(count)
                .as("Dependency '%s' should appear exactly once in the dependencies block", artifactId)
                .isEqualTo(1);
    }

    private List<String> readDependencyLines() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
        return content.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("implementation(")
                        || line.startsWith("testImplementation(")
                        || line.startsWith("testRuntimeOnly("))
                .toList();
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
