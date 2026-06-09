package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the dependency grouping and ordering convention in
 * configuration/build.gradle.kts. The PR added spring-boot-starter-test
 * as the first testImplementation entry, preceding junit-jupiter-api.
 * This test guards the convention that all production dependencies
 * ({@code implementation}) appear before all test dependencies
 * ({@code testImplementation}), and that the blank line separator
 * between groups is maintained. Violating this convention makes the
 * build file harder to audit for scope correctness.
 * <p>
 * Complements {@link ConfigurationBuildFileStructuralOrderTest}
 * (top-level block ordering), {@link BuildFileTestDependencyOrderingTest}
 * (test-dep internal ordering), and {@link BuildFileDependencyBlockStructureTest}
 * (block-level structure).
 */
class BuildFileDependencyGroupingConventionTest {

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
    void allImplementationDeps_shouldAppearBeforeTestImplementationDeps() {
        int lastImplLine = -1;
        int firstTestImplLine = Integer.MAX_VALUE;

        for (int i = 0; i < buildLines.size(); i++) {
            String trimmed = buildLines.get(i).trim();
            if (trimmed.startsWith("implementation(")) {
                lastImplLine = i;
            }
            if (trimmed.startsWith("testImplementation(") && i < firstTestImplLine) {
                firstTestImplLine = i;
            }
        }

        assertThat(lastImplLine)
                .as("Build file must contain at least one implementation dependency")
                .isGreaterThan(-1);
        assertThat(firstTestImplLine)
                .as("Build file must contain at least one testImplementation dependency")
                .isLessThan(Integer.MAX_VALUE);
        assertThat(lastImplLine)
                .as("All implementation deps must appear before all testImplementation deps — "
                        + "mixing scopes makes it harder to audit for scope correctness")
                .isLessThan(firstTestImplLine);
    }

    @Test
    void blankLine_shouldSeparateImplementationFromTestImplementationDeps() {
        int lastImplLine = -1;
        int firstTestImplLine = -1;

        for (int i = 0; i < buildLines.size(); i++) {
            String trimmed = buildLines.get(i).trim();
            if (trimmed.startsWith("implementation(")) {
                lastImplLine = i;
            }
            if (trimmed.startsWith("testImplementation(") && firstTestImplLine == -1) {
                firstTestImplLine = i;
            }
        }

        assertThat(lastImplLine).isGreaterThan(-1);
        assertThat(firstTestImplLine).isGreaterThan(-1);

        boolean hasBlankSeparator = false;
        for (int i = lastImplLine + 1; i < firstTestImplLine; i++) {
            if (buildLines.get(i).trim().isEmpty()) {
                hasBlankSeparator = true;
                break;
            }
        }
        assertThat(hasBlankSeparator)
                .as("A blank line must separate the last implementation dep from the first "
                        + "testImplementation dep for visual grouping clarity")
                .isTrue();
    }

    @Test
    void starterTest_shouldBeFirstTestImplementationDependency() {
        String firstTestImpl = buildLines.stream()
                .map(String::trim)
                .filter(line -> line.startsWith("testImplementation("))
                .findFirst()
                .orElse("");
        assertThat(firstTestImpl)
                .as("spring-boot-starter-test should be the first testImplementation dependency — "
                        + "it is the umbrella test starter and sets the foundation for all other test deps")
                .contains("spring-boot-starter-test");
    }

    @Test
    void pluginsBlock_shouldBeSeparatedFromDependenciesByBlankLine() {
        int pluginsClose = -1;
        int dependenciesOpen = -1;

        for (int i = 0; i < buildLines.size(); i++) {
            String trimmed = buildLines.get(i).trim();
            if (trimmed.equals("}") && pluginsClose == -1 && dependenciesOpen == -1) {
                pluginsClose = i;
            }
            if (trimmed.startsWith("dependencies")) {
                dependenciesOpen = i;
            }
        }

        assertThat(pluginsClose).isGreaterThan(-1);
        assertThat(dependenciesOpen).isGreaterThan(pluginsClose);

        boolean hasBlankSeparator = false;
        for (int i = pluginsClose + 1; i < dependenciesOpen; i++) {
            if (buildLines.get(i).trim().isEmpty()) {
                hasBlankSeparator = true;
                break;
            }
        }
        assertThat(hasBlankSeparator)
                .as("A blank line must separate the plugins block from the dependencies block "
                        + "for readability — this is a Kotlin DSL convention")
                .isTrue();
    }
}
