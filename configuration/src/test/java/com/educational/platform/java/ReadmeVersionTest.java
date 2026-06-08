package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the README documentation references Java 26 consistently.
 * <p>
 * When the Java version is upgraded, the README must be updated to reflect the
 * new version in the technology stack and installation instructions. This test
 * prevents documentation drift.
 */
public class ReadmeVersionTest {

    @Test
    void readme_shouldExist() {
        Path readme = findReadme();

        assertThat(readme)
                .as("README.md should exist at the project root")
                .exists();
    }

    @Test
    void readme_technologyStack_shouldReference_java26() throws IOException {
        Path readme = findReadme();
        String content = Files.readString(readme);

        assertThat(content)
                .as("README technology stack should list Java 26")
                .contains("Java 26");
    }

    @Test
    void readme_shouldNotReference_oldJavaVersion25() throws IOException {
        Path readme = findReadme();
        String content = Files.readString(readme);

        assertThat(content)
                .as("README should not reference old Java 25")
                .doesNotContain("Java 25");
    }

    @Test
    void readme_installInstructions_shouldReference_java26() throws IOException {
        Path readme = findReadme();
        String content = Files.readString(readme);

        assertThat(content)
                .as("README install instructions should reference Java 26")
                .containsPattern("(?i)install\\s+java\\s+26");
    }

    @Test
    void readme_shouldNotReference_oldJavaVersion21InInstallSection() throws IOException {
        Path readme = findReadme();
        String content = Files.readString(readme);

        assertThat(content)
                .as("README should not reference old Java 21 in install instructions")
                .doesNotContainPattern("(?i)install\\s+java\\s+21");
    }

    private Path findReadme() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("README.md");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Paths.get("README.md");
    }
}
