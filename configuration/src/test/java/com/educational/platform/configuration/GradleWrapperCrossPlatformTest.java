package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates cross-platform Gradle wrapper support. GradleWrapperConsistencyTest
 * covers the Unix wrapper (gradlew); this test covers the Windows batch file
 * (gradlew.bat) and common cross-platform concerns. The Spring Boot plugin's
 * bootRun task may be invoked on any platform via CI/CD; missing the Windows
 * wrapper would break Windows-based build environments.
 */
class GradleWrapperCrossPlatformTest {

    private static Path projectRoot;

    @BeforeAll
    static void findProjectRoot() {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
    }

    @Test
    void gradlewBat_shouldExist() {
        assertThat(Files.exists(projectRoot.resolve("gradlew.bat")))
                .as("gradlew.bat must exist for Windows CI/CD environments to run bootRun")
                .isTrue();
    }

    @Test
    void gradlewBat_shouldNotBeEmpty() throws IOException {
        Path gradlewBat = projectRoot.resolve("gradlew.bat");
        if (!Files.exists(gradlewBat)) {
            return;
        }
        assertThat(Files.size(gradlewBat))
                .as("gradlew.bat must not be empty")
                .isGreaterThan(0);
    }

    @Test
    void gradleWrapperJar_shouldNotBeEmpty() throws IOException {
        Path wrapperJar = projectRoot.resolve("gradle/wrapper/gradle-wrapper.jar");
        assertThat(Files.exists(wrapperJar))
                .as("gradle-wrapper.jar must exist")
                .isTrue();
        assertThat(Files.size(wrapperJar))
                .as("gradle-wrapper.jar must not be empty — an empty JAR would fail wrapper bootstrap")
                .isGreaterThan(0);
    }

    @Test
    void gradlewAndGradlewBat_shouldBothExist() {
        assertThat(Files.exists(projectRoot.resolve("gradlew")))
                .as("Unix wrapper (gradlew) must exist alongside Windows wrapper")
                .isTrue();
        assertThat(Files.exists(projectRoot.resolve("gradlew.bat")))
                .as("Windows wrapper (gradlew.bat) must exist alongside Unix wrapper")
                .isTrue();
    }

    @Test
    void gradleWrapperProperties_shouldEnableDistributionUrlValidation() throws IOException {
        String content = Files.readString(
                projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties"));
        assertThat(content)
                .as("gradle-wrapper.properties should enable validateDistributionUrl "
                        + "for integrity verification of the downloaded Gradle distribution")
                .containsPattern("validateDistributionUrl\\s*=\\s*true");
    }
}
