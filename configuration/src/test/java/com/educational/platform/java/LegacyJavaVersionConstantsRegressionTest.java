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
 * Regression guard ensuring no legacy Java version constants remain in any
 * build file across the project after the Java 26 upgrade.
 * <p>
 * {@link VersionUpgradeRegressionGuardTest} checks that the immediate
 * predecessor {@code VERSION_25} does not appear in the root build file.
 * This test extends that guard to <em>all</em> prior Java version constants
 * ({@code VERSION_17} through {@code VERSION_24}) across <em>all</em>
 * build files, including submodules. A submodule created from a template
 * or copied from another project could carry stale constants that compile
 * without error but silently produce lower-version bytecode.
 */
public class LegacyJavaVersionConstantsRegressionTest {

    private static final int CURRENT_JAVA_VERSION = 26;

    @ParameterizedTest(name = "No build file should contain VERSION_{0}")
    @ValueSource(ints = {17, 18, 19, 20, 21, 22, 23, 24, 25})
    void noBuildFile_shouldContain_legacyVersionConstant(int oldVersion) throws IOException {
        Path root = findProjectRoot();

        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> buildFiles = paths
                    .filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .toList();

            assertThat(buildFiles)
                    .as("Project should have at least one build.gradle.kts")
                    .isNotEmpty();

            for (Path buildFile : buildFiles) {
                String content = Files.readString(buildFile);
                assertThat(content)
                        .as("Build file %s should not contain VERSION_%d (current is VERSION_%d)",
                                root.relativize(buildFile), oldVersion, CURRENT_JAVA_VERSION)
                        .doesNotContain("VERSION_" + oldVersion);
            }
        }
    }

    @Test
    void rootBuildGradle_shouldContain_onlyCurrentVersionConstant() throws IOException {
        String content = Files.readString(findProjectRoot().resolve("build.gradle.kts"));

        long versionConstantCount = content.lines()
                .filter(line -> line.contains("JavaVersion.VERSION_"))
                .count();

        assertThat(versionConstantCount)
                .as("Root build.gradle.kts should have exactly 2 VERSION_ references (source + target)")
                .isEqualTo(2);

        for (String line : content.lines().toList()) {
            if (line.contains("JavaVersion.VERSION_")) {
                assertThat(line)
                        .as("Every JavaVersion.VERSION_ reference must be VERSION_%d", CURRENT_JAVA_VERSION)
                        .contains("VERSION_" + CURRENT_JAVA_VERSION);
            }
        }
    }

    @Test
    void noSettingsGradle_shouldReference_oldJavaVersion() throws IOException {
        Path settingsFile = findProjectRoot().resolve("settings.gradle.kts");
        if (Files.exists(settingsFile)) {
            String content = Files.readString(settingsFile);
            for (int v = 17; v < CURRENT_JAVA_VERSION; v++) {
                assertThat(content)
                        .as("settings.gradle.kts should not reference Java %d", v)
                        .doesNotContain("VERSION_" + v);
            }
        }
    }

    @ParameterizedTest(name = "README should not reference Java {0}")
    @ValueSource(ints = {17, 18, 19, 20, 21, 22, 23, 24, 25})
    void readme_shouldNotReference_legacyJavaVersion(int oldVersion) throws IOException {
        String content = Files.readString(findProjectRoot().resolve("README.md"));

        assertThat(content)
                .as("README.md should not reference Java %d", oldVersion)
                .doesNotContain("Java " + oldVersion);
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
