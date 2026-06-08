package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Gradle wrapper scripts (gradlew / gradlew.bat) use the
 * modern -jar invocation introduced in Gradle 9.5+.
 * <p>
 * Prior to Gradle 9.5, the wrapper used {@code -classpath ... org.gradle.wrapper.GradleWrapperMain}.
 * The new wrapper launches via {@code -jar gradle/wrapper/gradle-wrapper.jar} directly.
 * This test prevents accidental regression to the old CLASSPATH-based invocation.
 */
public class GradleWrapperScriptTest {

    @Test
    void gradlew_shouldUse_jarInvocation() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should use -jar to launch the wrapper")
                .contains("-jar");
    }

    @Test
    void gradlew_shouldNotUse_classpathInvocation() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should not define a CLASSPATH variable for the wrapper jar")
                .doesNotContain("CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar");

        assertThat(content)
                .as("gradlew should not invoke org.gradle.wrapper.GradleWrapperMain")
                .doesNotContain("org.gradle.wrapper.GradleWrapperMain");
    }

    @Test
    void gradlew_shouldReference_wrapperJarPath() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should reference the wrapper JAR via APP_HOME")
                .contains("$APP_HOME/gradle/wrapper/gradle-wrapper.jar");
    }

    @Test
    void gradlew_shouldContain_spdxLicenseIdentifier() throws IOException {
        Path gradlew = findProjectRoot().resolve("gradlew");
        String content = Files.readString(gradlew);

        assertThat(content)
                .as("gradlew should include the SPDX license identifier")
                .contains("SPDX-License-Identifier: Apache-2.0");
    }

    @Test
    void gradlew_shouldBeExecutable() {
        Path gradlew = findProjectRoot().resolve("gradlew");

        assertThat(gradlew)
                .as("gradlew should be executable")
                .isExecutable();
    }

    @Test
    void gradlewBat_shouldUse_jarInvocation() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should use -jar to launch the wrapper")
                .contains("-jar");
    }

    @Test
    void gradlewBat_shouldNotUse_classpathInvocation() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should not define a CLASSPATH variable")
                .doesNotContain("set CLASSPATH=");

        assertThat(content)
                .as("gradlew.bat should not invoke org.gradle.wrapper.GradleWrapperMain")
                .doesNotContain("org.gradle.wrapper.GradleWrapperMain");
    }

    @Test
    void gradlewBat_shouldUse_setlocalEnableExtensions() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should use 'setlocal EnableExtensions'")
                .contains("setlocal EnableExtensions");

        assertThat(content)
                .as("gradlew.bat should not use the old Windows_NT conditional setlocal")
                .doesNotContain("if \"%OS%\"==\"Windows_NT\" setlocal");
    }

    @Test
    void gradlewBat_shouldContain_spdxLicenseIdentifier() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should include the SPDX license identifier")
                .contains("SPDX-License-Identifier: Apache-2.0");
    }

    @Test
    void gradlewBat_shouldNotContain_oldFailLabel() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should not contain old :fail label")
                .doesNotContain(":fail");

        assertThat(content)
                .as("gradlew.bat should not contain old :end label")
                .doesNotContain(":end");

        assertThat(content)
                .as("gradlew.bat should not contain old :mainEnd label")
                .doesNotContain(":mainEnd");
    }

    @Test
    void gradlewBat_shouldUse_comspecExitForErrors() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should use COMSPEC /c exit for error handling")
                .contains("\"%COMSPEC%\" /c exit");
    }

    @Test
    void gradlewBat_shouldContain_exitWithErrorLevelLabel() throws IOException {
        Path gradlewBat = findProjectRoot().resolve("gradlew.bat");
        String content = Files.readString(gradlewBat);

        assertThat(content)
                .as("gradlew.bat should have the :exitWithErrorLevel label")
                .contains(":exitWithErrorLevel");
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
