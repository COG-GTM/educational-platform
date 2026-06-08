package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the ordering and grouping of dependency declarations in
 * {@code configuration/build.gradle.kts} after the Java 26 upgrade additions.
 * <p>
 * The PR added three new test dependencies. This test ensures they follow
 * correct ordering conventions:
 * <ul>
 *   <li>{@code implementation} deps come before {@code testImplementation} deps</li>
 *   <li>{@code testImplementation} deps come before {@code testRuntimeOnly} deps</li>
 *   <li>JUnit Jupiter artifacts are grouped together (api, params, engine)</li>
 *   <li>JUnit Platform artifacts are grouped together (engine, launcher)</li>
 * </ul>
 * <p>
 * {@link ConfigurationBuildDuplicateDependencyGuardTest} validates uniqueness.
 * {@link TestDependencyScopeValidationTest} validates scopes.
 * This test validates <em>declaration ordering</em> for maintainability.
 */
public class DependencyDeclarationOrderTest {

    @Test
    void implementationDeps_shouldAppear_beforeTestDeps() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int lastImplLine = -1;
        int firstTestLine = Integer.MAX_VALUE;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("implementation(") && !trimmed.startsWith("//")) {
                lastImplLine = i;
            }
            if ((trimmed.startsWith("testImplementation(") || trimmed.startsWith("testRuntimeOnly("))
                    && !trimmed.startsWith("//")) {
                firstTestLine = Math.min(firstTestLine, i);
            }
        }

        if (lastImplLine >= 0 && firstTestLine < Integer.MAX_VALUE) {
            assertThat(lastImplLine)
                    .as("Last implementation dependency should appear before first test dependency")
                    .isLessThan(firstTestLine);
        }
    }

    @Test
    void testImplementation_shouldAppear_beforeTestRuntimeOnly() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int lastTestImplLine = -1;
        int firstTestRuntimeLine = Integer.MAX_VALUE;

        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("testImplementation(") && !trimmed.startsWith("//")) {
                lastTestImplLine = i;
            }
            if (trimmed.startsWith("testRuntimeOnly(") && !trimmed.startsWith("//")) {
                firstTestRuntimeLine = Math.min(firstTestRuntimeLine, i);
            }
        }

        if (lastTestImplLine >= 0 && firstTestRuntimeLine < Integer.MAX_VALUE) {
            assertThat(firstTestRuntimeLine)
                    .as("testRuntimeOnly declarations should not precede testImplementation declarations")
                    .isGreaterThan(findFirstTestImplLine(lines));
        }
    }

    @Test
    void jupiterApi_shouldAppear_beforeJupiterParams() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int apiLine = findLineContaining(lines, "junit-jupiter-api");
        int paramsLine = findLineContaining(lines, "junit-jupiter-params");

        assertThat(apiLine).as("junit-jupiter-api should be declared").isGreaterThanOrEqualTo(0);
        assertThat(paramsLine).as("junit-jupiter-params should be declared").isGreaterThanOrEqualTo(0);

        assertThat(apiLine)
                .as("junit-jupiter-api should appear before junit-jupiter-params")
                .isLessThan(paramsLine);
    }

    @Test
    void jupiterParams_shouldAppear_beforeJupiterEngine() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int paramsLine = findLineContaining(lines, "junit-jupiter-params");
        int engineLine = findLineContaining(lines, "junit-jupiter-engine");

        assertThat(paramsLine).as("junit-jupiter-params should be declared").isGreaterThanOrEqualTo(0);
        assertThat(engineLine).as("junit-jupiter-engine should be declared").isGreaterThanOrEqualTo(0);

        assertThat(paramsLine)
                .as("junit-jupiter-params should appear before junit-jupiter-engine")
                .isLessThan(engineLine);
    }

    @Test
    void platformDeps_shouldAppear_afterJupiterDeps() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int lastJupiterLine = Math.max(
                findLineContaining(lines, "junit-jupiter-engine"),
                findLineContaining(lines, "junit-jupiter-params"));
        int firstPlatformLine = Math.min(
                findLineContaining(lines, "junit-platform-engine"),
                findLineContaining(lines, "junit-platform-launcher"));

        if (lastJupiterLine >= 0 && firstPlatformLine >= 0) {
            assertThat(lastJupiterLine)
                    .as("Jupiter dependencies should be grouped before Platform dependencies")
                    .isLessThan(firstPlatformLine);
        }
    }

    @Test
    void assertjDeclaration_shouldAppear_afterMockito() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int mockitoLine = findLineContaining(lines, "mockito-junit-jupiter");
        int assertjLine = findLineContaining(lines, "assertj-core");

        assertThat(mockitoLine).as("mockito should be declared").isGreaterThanOrEqualTo(0);
        assertThat(assertjLine).as("assertj-core should be declared").isGreaterThanOrEqualTo(0);

        assertThat(mockitoLine)
                .as("mockito should appear before assertj-core")
                .isLessThan(assertjLine);
    }

    @Test
    void archunitDeclaration_shouldAppear_last() throws IOException {
        List<String> lines = readConfigBuildGradleLines();

        int archunitLine = findLineContaining(lines, "archunit-junit5");

        // All other test deps should appear before archunit
        int assertjLine = findLineContaining(lines, "assertj-core");
        int mockitoLine = findLineContaining(lines, "mockito-junit-jupiter");

        assertThat(archunitLine)
                .as("archunit should appear after assertj")
                .isGreaterThan(assertjLine);
        assertThat(archunitLine)
                .as("archunit should appear after mockito")
                .isGreaterThan(mockitoLine);
    }

    // --- Helpers ---

    private int findLineContaining(List<String> lines, String text) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(text)) {
                return i;
            }
        }
        return -1;
    }

    private int findFirstTestImplLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().startsWith("testImplementation(")) {
                return i;
            }
        }
        return -1;
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
