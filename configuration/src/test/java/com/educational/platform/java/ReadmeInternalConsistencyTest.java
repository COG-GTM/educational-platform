package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates internal consistency of the README after the Java 26 upgrade.
 * <p>
 * The base branch had an inconsistency: the technology stack listed "Java 25"
 * while the install instructions said "Install Java 21". The upgrade fixed
 * this by changing both to Java 26. This test prevents such drift from
 * recurring by cross-referencing every Java version mention within the README.
 * <p>
 * {@link ReadmeVersionTest} validates that Java 26 is referenced.
 * {@link ReadmeHowToRunAccuracyTest} validates the "How to run" section.
 * {@link UpgradeCoherenceIntegrationTest} cross-references README with build
 * files. This test validates internal README consistency — all Java version
 * references within the document should agree with each other.
 */
public class ReadmeInternalConsistencyTest {

    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile(
            "(?i)\\bjava\\s+(\\d+)\\b");

    @Test
    void allJavaVersionReferences_shouldBe_consistent() throws IOException {
        String content = readReadme();
        Matcher matcher = JAVA_VERSION_PATTERN.matcher(content);

        List<String> versions = new ArrayList<>();
        while (matcher.find()) {
            versions.add(matcher.group(1));
        }

        assertThat(versions)
                .as("README should contain at least one Java version reference")
                .isNotEmpty();

        String expected = versions.getFirst();
        assertThat(versions)
                .as("All Java version references in README should be the same (%s)", expected)
                .allMatch(v -> v.equals(expected));
    }

    @Test
    void technologyStackJavaVersion_shouldMatch_installJavaVersion() throws IOException {
        String content = readReadme();

        // Extract version from technology stack section
        String techStackVersion = extractVersionFromSection(content, "Technology stack");
        // Extract version from "How to run" / install section
        String installVersion = extractVersionFromSection(content, "How to run");

        assertThat(techStackVersion)
                .as("Technology stack Java version should be extractable")
                .isNotNull();
        assertThat(installVersion)
                .as("Install section Java version should be extractable")
                .isNotNull();

        assertThat(techStackVersion)
                .as("Tech stack Java version should match install section Java version")
                .isEqualTo(installVersion);
    }

    @Test
    void noMixOfJavaVersions_shouldExist() throws IOException {
        String content = readReadme();

        // After the upgrade, the README should not contain any version other than 26
        assertThat(content).doesNotContain("Java 21");
        assertThat(content).doesNotContain("Java 25");
        assertThat(content).doesNotContain("Java 24");
        assertThat(content).doesNotContain("Java 23");
        assertThat(content).doesNotContain("Java 22");
        assertThat(content).doesNotContain("Java 17");
    }

    @Test
    void readme_shouldHave_bothTechStackAndInstallSections() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should have both Technology stack and How to run sections")
                .containsPattern("(?i)technology\\s+stack")
                .containsPattern("(?i)how\\s+to\\s+run");
    }

    @Test
    void downloadInstruction_shouldMatch_techStackVersion() throws IOException {
        String content = readReadme();
        Matcher downloadMatcher = Pattern.compile(
                "(?i)download\\s+and\\s+install\\s+java\\s+(\\d+)").matcher(content);
        Matcher techStackMatcher = Pattern.compile("Java\\s+(\\d+);").matcher(content);

        assertThat(downloadMatcher.find())
                .as("README should have a download instruction with a version")
                .isTrue();
        assertThat(techStackMatcher.find())
                .as("README should have a technology stack entry with Java version")
                .isTrue();

        assertThat(downloadMatcher.group(1))
                .as("Download instruction version should match tech stack version")
                .isEqualTo(techStackMatcher.group(1));
    }

    private String extractVersionFromSection(String content, String sectionName) {
        String[] lines = content.split("\n");
        boolean inSection = false;
        int sectionDepth = 0;
        for (String line : lines) {
            if (!inSection && line.toLowerCase().contains(sectionName.toLowerCase())) {
                inSection = true;
                sectionDepth = countLeadingHashes(line);
                // Also check the header line itself for a version reference
                Matcher m = JAVA_VERSION_PATTERN.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
                continue;
            }
            if (inSection && line.startsWith("#")) {
                int depth = countLeadingHashes(line);
                if (depth <= sectionDepth) {
                    break;
                }
                // Sub-header within the section — check it too
                Matcher m = JAVA_VERSION_PATTERN.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
                continue;
            }
            if (inSection) {
                Matcher m = JAVA_VERSION_PATTERN.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }

    private int countLeadingHashes(String line) {
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
