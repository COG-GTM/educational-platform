package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the copyright header and template reference in the Gradle wrapper
 * scripts after the 9.2.1 → 9.5.1 upgrade.
 * <p>
 * Gradle 9.5.1 updated the copyright year from "2015-2021" to "2015" (dropping
 * the end year) and pinned the template source URL to a specific Git commit
 * hash instead of referencing HEAD. These changes are part of Gradle's shift
 * to reproducible wrapper generation.
 * <p>
 * {@link GradleWrapperScriptTest} validates invocation patterns.
 * {@link GradleWrapperScriptModernPatternsTest} validates shell/batch patterns.
 * This test validates <em>header metadata</em>: copyright, license identifier,
 * and template provenance.
 */
public class GradleWrapperScriptCopyrightValidationTest {

    // --- gradlew copyright ---

    @Test
    void gradlew_copyright_shouldBe_2015() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should have copyright year '2015' (no end year)")
                .containsPattern("Copyright .+ 2015 the original authors");
    }

    @Test
    void gradlew_copyright_shouldNotContain_yearRange() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should not use old copyright year range '2015-2021'")
                .doesNotContain("2015-2021");
    }

    // --- gradlew template reference ---

    @Test
    void gradlew_templateUrl_shouldReference_specificCommit() throws IOException {
        String content = readGradlew();

        Pattern commitPattern = Pattern.compile(
                "https://github\\.com/gradle/gradle/blob/([0-9a-f]{40})/"
        );
        Matcher matcher = commitPattern.matcher(content);

        assertThat(matcher.find())
                .as("gradlew should reference a specific Git commit hash in the template URL")
                .isTrue();

        String commitHash = matcher.group(1);
        assertThat(commitHash)
                .as("Commit hash should be a 40-character hex string")
                .hasSize(40)
                .matches("[0-9a-f]+");
    }

    @Test
    void gradlew_templateUrl_shouldNotReference_head() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should not reference HEAD in the template URL (use specific commit)")
                .doesNotContain("gradle/blob/HEAD/");
    }

    @Test
    void gradlew_templateUrl_shouldReference_unixStartScript() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew template URL should reference unixStartScript.txt")
                .contains("unixStartScript.txt");
    }

    // --- gradlew.bat copyright ---

    @Test
    void gradlewBat_copyright_shouldNotContain_yearRange() throws IOException {
        String content = readGradlewBat();

        assertThat(content)
                .as("gradlew.bat should not use old copyright year range '2015-2021'")
                .doesNotContain("2015-2021");
    }

    // --- License consistency ---

    @Test
    void bothScripts_shouldHave_apacheLicense() throws IOException {
        String gradlew = readGradlew();
        String gradlewBat = readGradlewBat();

        assertThat(gradlew)
                .as("gradlew should mention Apache License")
                .contains("Apache License");

        assertThat(gradlewBat)
                .as("gradlew.bat should mention Apache License")
                .contains("Apache License");
    }

    @Test
    void bothScripts_shouldHave_spdxIdentifier() throws IOException {
        String gradlew = readGradlew();
        String gradlewBat = readGradlewBat();

        assertThat(gradlew)
                .as("gradlew should have SPDX-License-Identifier")
                .contains("SPDX-License-Identifier: Apache-2.0");

        assertThat(gradlewBat)
                .as("gradlew.bat should have SPDX-License-Identifier")
                .contains("SPDX-License-Identifier: Apache-2.0");
    }

    @Test
    void gradlew_shouldContain_gradleProjectUrl() throws IOException {
        String content = readGradlew();

        assertThat(content)
                .as("gradlew should reference the Gradle project URL")
                .contains("https://github.com/gradle/gradle/");
    }

    // --- helpers ---

    private String readGradlew() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradlew"));
    }

    private String readGradlewBat() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradlew.bat"));
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
