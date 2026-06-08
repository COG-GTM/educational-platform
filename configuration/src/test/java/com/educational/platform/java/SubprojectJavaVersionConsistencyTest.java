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
 * Validates that no subproject build.gradle.kts overrides the Java version
 * set by the root build script.
 * <p>
 * The root build.gradle.kts sets sourceCompatibility and targetCompatibility
 * to Java 26 for all subprojects. If any subproject overrides this, it could
 * produce bytecode at a different version — causing subtle runtime errors or
 * ArchUnit failures in that module.
 */
public class SubprojectJavaVersionConsistencyTest {

    @Test
    void noSubproject_shouldOverride_sourceCompatibility() throws IOException {
        List<Path> subprojectBuildFiles = findSubprojectBuildFiles();

        assertThat(subprojectBuildFiles)
                .as("Should find subproject build files to verify")
                .isNotEmpty();

        for (Path buildFile : subprojectBuildFiles) {
            String content = Files.readString(buildFile);

            assertThat(content)
                    .as("Subproject %s should not override sourceCompatibility (set by root)",
                            relativize(buildFile))
                    .doesNotContainPattern("sourceCompatibility\\s*=");
        }
    }

    @Test
    void noSubproject_shouldOverride_targetCompatibility() throws IOException {
        List<Path> subprojectBuildFiles = findSubprojectBuildFiles();

        for (Path buildFile : subprojectBuildFiles) {
            String content = Files.readString(buildFile);

            assertThat(content)
                    .as("Subproject %s should not override targetCompatibility (set by root)",
                            relativize(buildFile))
                    .doesNotContainPattern("targetCompatibility\\s*=");
        }
    }

    @Test
    void noSubproject_shouldReference_javaVersionConstant() throws IOException {
        List<Path> subprojectBuildFiles = findSubprojectBuildFiles();

        for (Path buildFile : subprojectBuildFiles) {
            String content = Files.readString(buildFile);

            assertThat(content)
                    .as("Subproject %s should not reference any JavaVersion constant (set by root)",
                            relativize(buildFile))
                    .doesNotContainPattern("JavaVersion\\.VERSION_\\d+");
        }
    }

    @Test
    void noSubproject_shouldReference_previousJavaVersion() throws IOException {
        List<Path> subprojectBuildFiles = findSubprojectBuildFiles();

        for (Path buildFile : subprojectBuildFiles) {
            String content = Files.readString(buildFile);

            assertThat(content)
                    .as("Subproject %s should not reference old Java 25",
                            relativize(buildFile))
                    .doesNotContain("VERSION_25")
                    .doesNotContain("java 25")
                    .doesNotContain("Java 25");
        }
    }

    @Test
    void rootBuildScript_shouldApply_javaVersionToAllSubprojects() throws IOException {
        Path rootBuild = findProjectRoot().resolve("build.gradle.kts");
        String content = Files.readString(rootBuild);

        assertThat(content)
                .as("Root build script should configure Java in a subprojects block")
                .contains("subprojects")
                .contains("sourceCompatibility")
                .contains("targetCompatibility");
    }

    @ParameterizedTest(name = "Subproject build file should exist: {0}")
    @ValueSource(strings = {
            "configuration/build.gradle.kts",
            "common/build.gradle.kts",
            "web/build.gradle.kts",
            "courses/application/build.gradle.kts",
            "courses/web/build.gradle.kts",
            "administration/application/build.gradle.kts",
            "course-enrollments/application/build.gradle.kts",
            "course-reviews/application/build.gradle.kts",
            "users/application/build.gradle.kts",
            "security/config/build.gradle.kts"
    })
    void subproject_buildFile_shouldExist(String relativePath) {
        Path buildFile = findProjectRoot().resolve(relativePath);

        assertThat(buildFile)
                .as("Subproject build file should exist: %s", relativePath)
                .exists();
    }

    private List<Path> findSubprojectBuildFiles() throws IOException {
        Path root = findProjectRoot();
        Path rootBuild = root.resolve("build.gradle.kts");

        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .filter(p -> !p.equals(rootBuild))
                    .filter(p -> !p.toString().contains(".gradle/"))
                    .toList();
        }
    }

    private String relativize(Path path) {
        return findProjectRoot().relativize(path).toString();
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
