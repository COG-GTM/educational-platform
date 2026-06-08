package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structural ordering and hierarchy of README.md sections
 * after the Java 26 upgrade edits.
 * <p>
 * {@link ReadmeVersionTest} and {@link ReadmeHowToRunAccuracyTest} validate
 * that the correct Java version appears in the README content.
 * {@link ReadmeDocumentationIntegrityTest} validates that key sections exist
 * and core technologies are listed.
 * <p>
 * This test validates <em>section ordering</em> — ensuring the Java 26 upgrade
 * edits did not accidentally reorder, merge, or break the heading hierarchy.
 * Specifically:
 * <ul>
 *   <li>Technology stack section appears before "How to run" section</li>
 *   <li>"Install Java" heading is a sub-heading under "How to run"</li>
 *   <li>"Run application" follows "Install Java" within "How to run"</li>
 *   <li>The heading level hierarchy is maintained (no skipped levels)</li>
 * </ul>
 */
public class ReadmeSectionStructureTest {

    @Test
    void technologyStack_shouldAppearBefore_howToRun() throws IOException {
        String content = readReadme();

        int techStackPos = content.indexOf("Technology stack");
        int howToRunPos = content.indexOf("How to run");

        assertThat(techStackPos)
                .as("Technology stack section should exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(howToRunPos)
                .as("How to run section should exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(techStackPos)
                .as("Technology stack should appear before How to run")
                .isLessThan(howToRunPos);
    }

    @Test
    void installJava_shouldAppearBefore_runApplication() throws IOException {
        String content = readReadme();

        int installPos = findCaseInsensitive(content, "Install Java 26");
        int runAppPos = findCaseInsensitive(content, "Run application");

        assertThat(installPos)
                .as("'Install Java 26' section should exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(runAppPos)
                .as("'Run application' section should exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(installPos)
                .as("'Install Java 26' should appear before 'Run application'")
                .isLessThan(runAppPos);
    }

    @Test
    void installJava_shouldBe_subSectionOf_howToRun() throws IOException {
        List<String> lines = Files.readAllLines(findProjectRoot().resolve("README.md"));

        int howToRunLine = -1;
        int installJavaLine = -1;
        int howToRunLevel = 0;
        int installJavaLevel = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.contains("How to run")) {
                howToRunLine = i;
                howToRunLevel = countHeadingLevel(line);
            }
            if (line.toLowerCase().contains("install java 26")) {
                installJavaLine = i;
                installJavaLevel = countHeadingLevel(line);
            }
        }

        assertThat(howToRunLine).as("'How to run' heading should exist").isGreaterThanOrEqualTo(0);
        assertThat(installJavaLine).as("'Install Java 26' heading should exist").isGreaterThanOrEqualTo(0);

        assertThat(installJavaLine)
                .as("'Install Java 26' should appear after 'How to run'")
                .isGreaterThan(howToRunLine);

        if (howToRunLevel > 0 && installJavaLevel > 0) {
            assertThat(installJavaLevel)
                    .as("'Install Java 26' heading level (%d) should be deeper than 'How to run' (%d)",
                            installJavaLevel, howToRunLevel)
                    .isGreaterThan(howToRunLevel);
        }
    }

    @Test
    void readmeHeadings_shouldNotSkipLevels() throws IOException {
        List<String> lines = Files.readAllLines(findProjectRoot().resolve("README.md"));
        int previousLevel = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                int level = countHeadingLevel(trimmed);
                if (level > 0 && previousLevel > 0) {
                    // Heading level should not jump by more than 1 (e.g., ## to ####)
                    assertThat(level)
                            .as("Heading '%s' at level %d should not skip levels from previous level %d",
                                    trimmed.substring(0, Math.min(trimmed.length(), 50)), level, previousLevel)
                            .isLessThanOrEqualTo(previousLevel + 1);
                }
                if (level > 0) {
                    previousLevel = level;
                }
            }
        }
    }

    @ParameterizedTest(name = "README should contain section: {0}")
    @ValueSource(strings = {
            "The goals of this application",
            "Architecture",
            "Technology stack",
            "How to run",
            "Contribution",
            "License"
    })
    void readme_shouldContain_expectedSections(String section) throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should contain section: %s", section)
                .contains(section);
    }

    @Test
    void technologyStackItems_shouldBe_inListFormat() throws IOException {
        String content = readReadme();

        // The technology stack should list items with markdown bullet points
        int techStackStart = content.indexOf("Technology stack");
        assertThat(techStackStart).isGreaterThanOrEqualTo(0);

        String afterTechStack = content.substring(techStackStart);
        assertThat(afterTechStack)
                .as("Technology stack should use markdown list format with '- ' prefix")
                .contains("- Spring")
                .contains("- Java 26")
                .contains("- ArchUnit")
                .contains("- Gradle");
    }

    @Test
    void javaVersionInTechStack_shouldBe_onOwnLine() throws IOException {
        List<String> lines = Files.readAllLines(findProjectRoot().resolve("README.md"));

        boolean foundJava26Line = lines.stream()
                .map(String::trim)
                .anyMatch(line -> line.equals("- Java 26;"));

        assertThat(foundJava26Line)
                .as("'- Java 26;' should be on its own line in the technology stack list")
                .isTrue();
    }

    private int findCaseInsensitive(String content, String target) {
        return content.toLowerCase().indexOf(target.toLowerCase());
    }

    private int countHeadingLevel(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '#') count++;
            else break;
        }
        return count;
    }

    private String readReadme() throws IOException {
        return Files.readString(findProjectRoot().resolve("README.md"));
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
