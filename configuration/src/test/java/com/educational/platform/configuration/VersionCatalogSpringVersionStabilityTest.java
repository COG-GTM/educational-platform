package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring version declared in gradle/libs.versions.toml is a
 * stable GA release. Using snapshot, milestone, or release candidate versions in
 * the version catalog would make the build non-reproducible and potentially
 * introduce breaking API changes. The Spring Boot plugin and BOM both derive
 * their version from this single key, so an unstable version would affect the
 * entire dependency tree.
 * <p>
 * Complements {@link SpringBootPluginVersionTest} which validates the runtime
 * version, and {@link PluginBomRuntimeVersionCrossValidationTest} which
 * cross-validates the TOML→BOM→runtime pipeline.
 */
class VersionCatalogSpringVersionStabilityTest {

    private static String springVersion;

    @BeforeAll
    static void loadVersionCatalog() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        String tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
        Matcher m = Pattern.compile("^spring\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE)
                .matcher(tomlContent);
        assertThat(m.find())
                .as("libs.versions.toml must declare a 'spring' version key")
                .isTrue();
        springVersion = m.group(1);
    }

    @Test
    void springVersion_shouldNotBeSnapshot() {
        assertThat(springVersion)
                .as("Spring version must NOT be a SNAPSHOT — "
                        + "snapshots are non-reproducible and may introduce breaking changes")
                .doesNotContainIgnoringCase("SNAPSHOT");
    }

    @Test
    void springVersion_shouldNotBeMilestone() {
        assertThat(springVersion)
                .as("Spring version must NOT be a milestone (M1, M2, etc.) — "
                        + "milestones may have incomplete features and breaking API changes")
                .doesNotMatch(".*\\.M\\d+$");
    }

    @Test
    void springVersion_shouldNotBeReleaseCandidate() {
        assertThat(springVersion)
                .as("Spring version must NOT be a release candidate (RC1, RC2, etc.) — "
                        + "RCs may still have unresolved issues")
                .doesNotMatch(".*\\.RC\\d+$")
                .doesNotMatch(".*-RC\\d+$");
    }

    @Test
    void springVersion_shouldFollowSemanticVersioning() {
        assertThat(springVersion)
                .as("Spring version must follow semantic versioning (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+");
    }

    @Test
    void springVersion_majorVersion_shouldBe4() {
        assertThat(springVersion)
                .as("Spring Boot version must be 4.x to align with the Spring Boot plugin "
                        + "declared in the plugins block")
                .startsWith("4.");
    }

    @Test
    void springVersion_shouldNotContainBuildMetadata() {
        assertThat(springVersion)
                .as("Spring version must not contain build metadata (+build) — "
                        + "Gradle's version resolution does not support build metadata in coordinates")
                .doesNotContain("+");
    }
}
