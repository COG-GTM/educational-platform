package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the README's "How to run" section is accurate and consistent
 * with the actual project configuration after the Java 26 upgrade.
 * <p>
 * The Java 26 upgrade changed the install instructions from "Install Java 21"
 * to "Install Java 26". This test ensures the README correctly reflects:
 * - The current Java version requirement
 * - The Gradle wrapper command for running the application
 * - No stale references to previous Java versions in the instructions
 */
public class ReadmeHowToRunAccuracyTest {

    @Test
    void readme_installSection_shouldReference_java26() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README install section should reference Java 26")
                .containsPattern("(?i)install\\s+java\\s+26");
    }

    @Test
    void readme_installSection_shouldNotReference_java21() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README install section should not reference old Java 21")
                .doesNotContainPattern("(?i)install\\s+java\\s+21");
    }

    @Test
    void readme_installSection_shouldNotReference_java25() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README install section should not reference previous Java 25")
                .doesNotContainPattern("(?i)install\\s+java\\s+25");
    }

    @Test
    void readme_shouldContain_gradlewBootRunCommand() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should contain ./gradlew bootRun command")
                .contains("./gradlew bootRun");
    }

    @Test
    void readme_shouldContain_runApplicationSection() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README should have a 'Run application' section")
                .containsPattern("(?i)run\\s+application");
    }

    @Test
    void readme_technologyStack_shouldList_java26() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README technology stack should list Java 26")
                .contains("Java 26");
    }

    @Test
    void readme_technologyStack_shouldNotList_java25() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README technology stack should not list Java 25")
                .doesNotContain("Java 25");
    }

    @Test
    void readme_technologyStack_shouldList_archunit() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README technology stack should mention ArchUnit")
                .contains("ArchUnit");
    }

    @Test
    void readme_technologyStack_shouldList_gradle() throws IOException {
        String content = readReadme();

        assertThat(content)
                .as("README technology stack should mention Gradle")
                .contains("Gradle");
    }

    @Test
    void readme_shouldBeEncodedIn_utf8() throws IOException {
        Path readme = findProjectRoot().resolve("README.md");
        byte[] bytes = Files.readAllBytes(readme);

        String decoded = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        byte[] reEncoded = decoded.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(reEncoded)
                .as("README.md should round-trip through UTF-8")
                .isEqualTo(bytes);
    }

    @Test
    void readme_shouldNotContain_outdatedJavaDownloadInstructions() throws IOException {
        String content = readReadme();

        // After the upgrade, the README should say "Download and install Java 26"
        assertThat(content)
                .as("README should say download and install Java 26")
                .containsPattern("(?i)download\\s+and\\s+install\\s+java\\s+26");

        assertThat(content)
                .as("README should not say download and install Java 21")
                .doesNotContainPattern("(?i)download\\s+and\\s+install\\s+java\\s+21");

        assertThat(content)
                .as("README should not say download and install Java 25")
                .doesNotContainPattern("(?i)download\\s+and\\s+install\\s+java\\s+25");
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
