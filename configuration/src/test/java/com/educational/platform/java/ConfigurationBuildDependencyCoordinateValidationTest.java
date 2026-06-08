package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the exact Maven coordinates (groupId, artifactId) of the three
 * test dependencies added in the Java 26 upgrade.
 * <p>
 * Existing tests ({@link NewTestDependencyClasspathTest},
 * {@link TestDependencyScopeValidationTest}) verify classpath availability
 * and correct scope via substring matching (e.g., {@code contains("junit-jupiter-params")}).
 * A typo in the groupId (e.g., "org.junit" instead of "org.junit.jupiter")
 * would pass those checks but fail at dependency resolution time.
 * <p>
 * This test validates the complete coordinate pair for each new dependency
 * as declared in {@code configuration/build.gradle.kts}.
 */
public class ConfigurationBuildDependencyCoordinateValidationTest {

    @ParameterizedTest(name = "New dependency should have correct coordinates: {0}:{1}")
    @CsvSource({
            "org.junit.jupiter,  junit-jupiter-params",
            "org.junit.jupiter,  junit-jupiter-engine",
            "org.assertj,        assertj-core"
    })
    void newDependency_shouldHave_correctGroupAndArtifact(String groupId, String artifactId) throws IOException {
        String content = readConfigBuildGradle();

        Pattern coordPattern = Pattern.compile(
                "\\(\\s*\"" + Pattern.quote(groupId) + "\"\\s*,\\s*\"" + Pattern.quote(artifactId) + "\"");

        assertThat(coordPattern.matcher(content).find())
                .as("configuration/build.gradle.kts should declare (\"%s\", \"%s\")", groupId, artifactId)
                .isTrue();
    }

    @Test
    void jupiterParams_groupId_shouldBeJupiter_notPlatform() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        for (String line : lines) {
            if (line.contains("junit-jupiter-params")) {
                assertThat(line)
                        .as("junit-jupiter-params should use org.junit.jupiter groupId, not org.junit.platform")
                        .contains("org.junit.jupiter")
                        .doesNotContain("org.junit.platform");
            }
        }
    }

    @Test
    void jupiterEngine_groupId_shouldBeJupiter_notPlatform() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        for (String line : lines) {
            if (line.contains("junit-jupiter-engine")) {
                assertThat(line)
                        .as("junit-jupiter-engine should use org.junit.jupiter groupId, not org.junit.platform")
                        .contains("org.junit.jupiter")
                        .doesNotContain("org.junit.platform");
            }
        }
    }

    @Test
    void assertjCore_groupId_shouldBeOrgAssertj() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        for (String line : lines) {
            if (line.contains("assertj-core")) {
                assertThat(line)
                        .as("assertj-core should use org.assertj groupId")
                        .contains("org.assertj");
            }
        }
    }

    @Test
    void allNewDependencies_shouldUse_twoArgCoordinateFormat() throws IOException {
        String content = readConfigBuildGradle();

        // The project uses the (groupId, artifactId) two-arg format, not
        // the "groupId:artifactId:version" single-string format.
        // Ensure new dependencies follow the same convention.
        String[] newArtifacts = {"junit-jupiter-params", "junit-jupiter-engine", "assertj-core"};
        for (String artifact : newArtifacts) {
            assertThat(content)
                    .as("'%s' should not use colon-separated coordinate format", artifact)
                    .doesNotContainPattern("\"[^\"]*:" + Pattern.quote(artifact) + ":[^\"]*\"");
        }
    }

    @Test
    void existingDependencies_shouldRetain_correctGroupIds() throws IOException {
        String content = readConfigBuildGradle();

        // Verify the pre-existing test deps were not accidentally corrupted
        assertThat(content)
                .as("junit-jupiter-api should retain org.junit.jupiter groupId")
                .containsPattern("\"org\\.junit\\.jupiter\"\\s*,\\s*\"junit-jupiter-api\"");

        assertThat(content)
                .as("junit-platform-engine should retain org.junit.platform groupId")
                .containsPattern("\"org\\.junit\\.platform\"\\s*,\\s*\"junit-platform-engine\"");

        assertThat(content)
                .as("mockito-junit-jupiter should retain org.mockito groupId")
                .containsPattern("\"org\\.mockito\"\\s*,\\s*\"mockito-junit-jupiter\"");

        assertThat(content)
                .as("archunit-junit5 should retain com.tngtech.archunit groupId")
                .containsPattern("\"com\\.tngtech\\.archunit\"\\s*,\\s*\"archunit-junit5\"");
    }

    private String readConfigBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
    }

    private List<String> readConfigBuildGradleLines() throws IOException {
        return Files.readAllLines(findProjectRoot().resolve("configuration/build.gradle.kts"));
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
