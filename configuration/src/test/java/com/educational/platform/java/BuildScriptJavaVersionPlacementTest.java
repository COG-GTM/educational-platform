package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the placement and context of the Java version declaration in the
 * root build.gradle.kts.
 * <p>
 * The PR changed {@code JavaVersion.VERSION_25} to {@code VERSION_26} in
 * the root build script's {@code java {}} block within the {@code subprojects}
 * closure. This test ensures:
 * <ul>
 *   <li>The version appears inside a proper {@code java {}} block</li>
 *   <li>Both source and target compatibility are set together (atomic pair)</li>
 *   <li>No stray VERSION_25 or VERSION_26 references exist outside the java block</li>
 *   <li>The java block is inside the subprojects closure (applies to all modules)</li>
 * </ul>
 * <p>
 * {@link RootBuildScriptStructureTest} validates overall script structure.
 * {@link SubprojectJavaVersionConsistencyTest} validates subprojects don't override.
 * This test validates the <em>placement context</em> of the version declaration.
 */
public class BuildScriptJavaVersionPlacementTest {

    @Test
    void javaVersionDeclaration_shouldAppear_insideJavaBlock() throws IOException {
        String content = readRootBuildGradle();

        // Find java { ... } block and verify VERSION_26 is inside it
        Pattern javaBlock = Pattern.compile("java\\s*\\{[^}]*VERSION_26[^}]*}", Pattern.DOTALL);
        assertThat(javaBlock.matcher(content).find())
                .as("VERSION_26 should appear inside a java { } block")
                .isTrue();
    }

    @Test
    void sourceAndTargetCompatibility_shouldBeAdjacent() throws IOException {
        List<String> lines = Files.readAllLines(findProjectRoot().resolve("build.gradle.kts"));

        int sourceLineIndex = -1;
        int targetLineIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("sourceCompatibility")) {
                sourceLineIndex = i;
            }
            if (lines.get(i).contains("targetCompatibility")) {
                targetLineIndex = i;
            }
        }

        assertThat(sourceLineIndex)
                .as("sourceCompatibility should be declared in the build script")
                .isGreaterThanOrEqualTo(0);
        assertThat(targetLineIndex)
                .as("targetCompatibility should be declared in the build script")
                .isGreaterThanOrEqualTo(0);

        int distance = Math.abs(targetLineIndex - sourceLineIndex);
        assertThat(distance)
                .as("source and target compatibility should be adjacent (within 1 line)")
                .isLessThanOrEqualTo(1);
    }

    @Test
    void javaBlock_shouldBe_insideSubprojects() throws IOException {
        List<String> lines = Files.readAllLines(findProjectRoot().resolve("build.gradle.kts"));

        // Find the line containing "subprojects {" and then verify
        // "java {" appears after it and before the end of that block
        int subprojectsLine = -1;
        int javaVersionLine = -1;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith("subprojects")) {
                subprojectsLine = i;
            }
            if (lines.get(i).contains("VERSION_26")) {
                javaVersionLine = i;
            }
        }

        assertThat(subprojectsLine)
                .as("subprojects block should exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(javaVersionLine)
                .as("VERSION_26 should exist in the build script")
                .isGreaterThanOrEqualTo(0);
        assertThat(javaVersionLine)
                .as("VERSION_26 should appear after the subprojects { opening")
                .isGreaterThan(subprojectsLine);
    }

    @Test
    void version26_shouldNotAppear_outsideJavaBlock() throws IOException {
        String content = readRootBuildGradle();

        // Count total occurrences of VERSION_26
        long totalCount = Pattern.compile("VERSION_26").matcher(content).results().count();

        // Should be exactly 2 (sourceCompatibility + targetCompatibility)
        assertThat(totalCount)
                .as("VERSION_26 should appear exactly twice (source + target compatibility)")
                .isEqualTo(2);
    }

    @Test
    void buildGradle_shouldNotContain_version25() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("build.gradle.kts should not contain any reference to VERSION_25")
                .doesNotContain("VERSION_25");
    }

    @ParameterizedTest(name = "Old Java version constant VERSION_{0} should not be present")
    @ValueSource(ints = {8, 11, 17, 21, 22, 23, 24, 25})
    void buildGradle_shouldNotContain_olderJavaVersionConstants(int version) throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("build.gradle.kts should not reference VERSION_%d", version)
                .doesNotContain("VERSION_" + version);
    }

    @Test
    void javaBlock_shouldOnlyContain_compatibilityDeclarations() throws IOException {
        String content = readRootBuildGradle();

        // Extract the java { } block content
        Pattern javaBlock = Pattern.compile("java\\s*\\{([^}]*)}", Pattern.DOTALL);
        Matcher matcher = javaBlock.matcher(content);
        assertThat(matcher.find()).isTrue();

        String javaBlockContent = matcher.group(1).trim();
        List<String> nonEmptyLines = javaBlockContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        // The java block should only contain sourceCompatibility and targetCompatibility
        assertThat(nonEmptyLines)
                .as("java { } block should only contain compatibility declarations")
                .hasSize(2)
                .allMatch(line -> line.contains("Compatibility"));
    }

    private String readRootBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("build.gradle.kts"));
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
