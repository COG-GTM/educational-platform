package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the version management consistency between Spring Boot starters
 * and manually-versioned test dependencies in configuration/build.gradle.kts.
 * The PR added spring-boot-starter-test without an explicit version (relying
 * on the BOM via io.spring.dependency-management), while non-BOM-managed
 * dependencies (mockito-junit-jupiter, archunit-junit5) use version catalog
 * references via {@code libs.versions.*.get()}.
 * <p>
 * These tests guard against:
 * <ul>
 *   <li>BOM-managed dependencies accidentally getting explicit versions</li>
 *   <li>Non-BOM dependencies missing their version catalog reference</li>
 *   <li>Inconsistent versioning strategies within the same build file</li>
 * </ul>
 * <p>
 * Complements {@link StarterTestDependencyNotationValidationTest} (which
 * validates starter-test GAV format) and {@link VersionCatalogSpringVersionUnificationTest}
 * (which validates version catalog consistency). This test validates the
 * cross-cutting concern of which dependencies use BOM vs catalog versions.
 */
class BuildFileDependencyVersionManagementConsistencyTest {

    private static String buildContent;
    private static List<String> buildLines;

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
        buildLines = buildContent.lines().toList();
    }

    @Test
    void springBootStarters_shouldNotHaveExplicitVersions() {
        List<String> starterLines = buildLines.stream()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("spring-boot-starter"))
                .toList();
        for (String line : starterLines) {
            long commaCount = line.chars().filter(ch -> ch == ',').count();
            assertThat(commaCount)
                    .as("Spring Boot starter '%s' must use two-arg (group, artifact) notation "
                            + "without a third version argument — version is managed by the BOM",
                            line.trim())
                    .isLessThanOrEqualTo(1);
        }
    }

    @Test
    void manuallyVersionedDeps_shouldUseVersionCatalogRefs() {
        Pattern versionCatalogPattern = Pattern.compile("libs\\.versions\\.[\\w.]+\\.get\\(\\)");
        List<String> testImplLines = buildLines.stream()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.trim().startsWith("testImplementation("))
                .toList();
        for (String line : testImplLines) {
            // Count commas to detect three-arg notation (group, artifact, version)
            long commaCount = line.chars().filter(ch -> ch == ',').count();
            if (commaCount >= 2) {
                Matcher matcher = versionCatalogPattern.matcher(line);
                assertThat(matcher.find())
                        .as("Manually-versioned dependency must use libs.versions.*.get() "
                                + "for version catalog management: %s", line.trim())
                        .isTrue();
            }
        }
    }

    @Test
    void implementationDeps_projectRefs_shouldNotHaveVersions() {
        List<String> projectLines = buildLines.stream()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("project(\":"))
                .toList();
        for (String line : projectLines) {
            assertThat(line)
                    .as("project() dependencies must not have explicit versions — "
                            + "module versions are defined at the root level: %s", line.trim())
                    .doesNotContainPattern("project\\(\"[^\"]+\"\\)\\s*,\\s*\"[\\d.]");
        }
    }

    @Test
    void junitJupiterApi_shouldNotHaveExplicitVersion() {
        List<String> jupiterLines = buildLines.stream()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("junit-jupiter-api"))
                .toList();
        for (String line : jupiterLines) {
            long commaCount = line.chars().filter(ch -> ch == ',').count();
            assertThat(commaCount)
                    .as("junit-jupiter-api must not have a third version argument — "
                            + "JUnit 5 version is managed by the Spring Boot BOM: %s", line.trim())
                    .isLessThanOrEqualTo(1);
        }
    }

    @Test
    void junitPlatformDeps_shouldNotHaveExplicitVersions() {
        List<String> platformLines = buildLines.stream()
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("junit-platform"))
                .toList();
        for (String line : platformLines) {
            long commaCount = line.chars().filter(ch -> ch == ',').count();
            assertThat(commaCount)
                    .as("JUnit Platform dependency must not have a third version argument — "
                            + "JUnit Platform version is managed by the Spring Boot BOM: %s", line.trim())
                    .isLessThanOrEqualTo(1);
        }
    }
}
