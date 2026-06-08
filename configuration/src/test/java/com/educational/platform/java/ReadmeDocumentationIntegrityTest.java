package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the structural integrity of README.md after the Java 26 upgrade.
 * <p>
 * The PR edits two sections: the technology stack (Java 25 to Java 26)
 * and the install instructions (Java 21 to Java 26). This test ensures
 * those edits did not accidentally remove other content or break the
 * document structure.
 */
public class ReadmeDocumentationIntegrityTest {

    @ParameterizedTest(name = "README should reference technology: {0}")
    @ValueSource(strings = {"Spring", "ArchUnit", "Gradle"})
    void readme_shouldStillReference_coreTechnologies(String technology) throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README technology stack should still reference %s", technology)
                .contains(technology);
    }

    @Test
    void readme_shouldContain_technologyStackSection() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should have a Technology stack section")
                .containsPattern("(?i)technology\\s+stack");
    }

    @Test
    void readme_shouldContain_howToRunSection() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should have a 'How to run' section")
                .containsPattern("(?i)how\\s+to\\s+run");
    }

    @Test
    void readme_shouldContain_gradlewBootRunCommand() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should still document the gradlew bootRun command")
                .contains("gradlew bootRun");
    }

    @Test
    void readme_shouldNotBeEmpty() throws IOException {
        String content = readReadme();

        assertThat(content.length())
                .as("README should have substantial content")
                .isGreaterThan(500);
    }

    @Test
    void readme_java26Reference_shouldBeInTechStackAndInstall() throws IOException {
        String content = readReadme();

        long java26Count = content.lines()
                .filter(line -> line.contains("Java 26"))
                .count();

        assertThat(java26Count)
                .as("Java 26 should appear at least twice (tech stack and install section)")
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void readme_shouldNotContain_anyStaleJavaVersionReferences() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should not reference old Java 25")
                .doesNotContain("Java 25");

        assertThat(content)
                .as("README install section should not reference Java 21")
                .doesNotContainPattern("(?i)install\\s+java\\s+21");
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
