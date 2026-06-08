package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that Spring BOM-managed dependencies do NOT specify explicit
 * version strings in {@code configuration/build.gradle.kts}.
 * <p>
 * The Java 26 upgrade added {@code junit-jupiter-params} and
 * {@code junit-jupiter-engine} alongside the pre-existing
 * {@code junit-jupiter-api}, {@code junit-platform-engine}, and
 * {@code junit-platform-launcher}. All five are managed by the Spring
 * Boot BOM ({@code spring-boot-dependencies}) and must NOT declare
 * explicit versions. Adding an explicit version to one BOM-managed
 * artifact overrides the BOM for that artifact alone, causing silent
 * version drift and potential {@code NoSuchMethodError} at runtime.
 * <p>
 * In contrast, {@code mockito-junit-jupiter}, {@code assertj-core}, and
 * {@code archunit-junit5} are NOT managed by the Spring BOM and must
 * specify versions via the Gradle version catalog.
 * <p>
 * {@link TestDependencyScopeValidationTest} validates correct scopes.
 * {@link JUnitPlatformBomAlignmentTest} validates resolved version alignment.
 * This test validates that the <em>declaration</em> does not override BOM
 * management with explicit versions.
 */
public class BomManagedDependencyNoExplicitVersionTest {

    private static final Pattern VERSION_LITERAL = Pattern.compile(
            "\"\\d+\\.\\d+");

    @ParameterizedTest(name = "BOM-managed dependency ''{0}'' should not have an explicit version")
    @ValueSource(strings = {
            "junit-jupiter-api",
            "junit-jupiter-params",
            "junit-platform-engine",
            "junit-platform-launcher"
    })
    void bomManagedDep_shouldNotSpecify_explicitVersion(String artifactId) throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains(artifactId)
                    && (trimmed.startsWith("testImplementation") || trimmed.startsWith("testRuntimeOnly"))) {
                assertThat(trimmed)
                        .as("BOM-managed '%s' should not have a version argument (third string or libs.versions.*)",
                                artifactId)
                        .doesNotContain("libs.versions.")
                        .doesNotContainPattern(VERSION_LITERAL);
            }
        }
    }

    @ParameterizedTest(name = "Catalog-managed dependency ''{0}'' should reference version catalog")
    @ValueSource(strings = {
            "mockito-junit-jupiter",
            "assertj-core",
            "archunit-junit5"
    })
    void catalogManagedDep_shouldReference_versionCatalog(String artifactId) throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        boolean found = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains(artifactId)
                    && (trimmed.startsWith("testImplementation") || trimmed.startsWith("testRuntimeOnly"))) {
                found = true;
                assertThat(trimmed)
                        .as("Non-BOM '%s' should reference version catalog", artifactId)
                        .contains("libs.versions.");
            }
        }

        assertThat(found)
                .as("Dependency '%s' should be declared in build file", artifactId)
                .isTrue();
    }

    @Test
    void jupiterEngine_testRuntimeOnly_shouldNotSpecify_explicitVersion() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("testRuntimeOnly") && trimmed.contains("junit-jupiter-engine")) {
                assertThat(trimmed)
                        .as("junit-jupiter-engine (testRuntimeOnly) should rely on BOM, no explicit version")
                        .doesNotContain("libs.versions.")
                        .doesNotContainPattern(VERSION_LITERAL);
            }
        }
    }

    @Test
    void bomManagedDeps_shouldUse_twoArgForm() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.contains("junit-jupiter-api") || trimmed.contains("junit-jupiter-params")
                    || trimmed.contains("junit-platform-engine") || trimmed.contains("junit-platform-launcher"))
                    && trimmed.startsWith("testImplementation")) {
                long commaCount = trimmed.chars().filter(c -> c == ',').count();
                assertThat(commaCount)
                        .as("BOM-managed dep line '%s' should have exactly 1 comma (group, artifact), not 2 (group, artifact, version)",
                                trimmed)
                        .isEqualTo(1);
            }
        }
    }

    private List<String> readConfigBuildGradleLines() throws IOException {
        Path configBuild = findProjectRoot().resolve("configuration/build.gradle.kts");
        return Files.readAllLines(configBuild);
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
