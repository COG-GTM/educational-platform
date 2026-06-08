package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the build-isolation invariant that spring-boot-starter-test is always
 * scoped as testImplementation wherever it is declared. Using a broader scope
 * (implementation, api, runtimeOnly) would leak test infrastructure into
 * the production classpath. Also validates that the Spring Boot plugin itself
 * is NOT applied in library modules — only in configuration.
 */
class StarterTestModuleIsolationTest {

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

    @ParameterizedTest
    @ValueSource(strings = {
            "courses/application/build.gradle.kts",
            "courses/web/build.gradle.kts",
            "courses/integration-events/build.gradle.kts",
            "administration/application/build.gradle.kts",
            "administration/web/build.gradle.kts",
            "administration/integration-events/build.gradle.kts",
            "course-enrollments/application/build.gradle.kts",
            "course-enrollments/web/build.gradle.kts",
            "course-enrollments/integration-events/build.gradle.kts",
            "course-reviews/application/build.gradle.kts",
            "course-reviews/web/build.gradle.kts",
            "course-reviews/integration-events/build.gradle.kts",
            "users/application/build.gradle.kts",
            "users/web/build.gradle.kts",
            "users/integration-events/build.gradle.kts",
            "common/build.gradle.kts",
            "web/build.gradle.kts",
            "configuration/build.gradle.kts"
    })
    void starterTest_shouldAlwaysBeTestImplementation(String buildFilePath) throws IOException {
        Path buildFile = projectRoot.resolve(buildFilePath);
        if (!Files.exists(buildFile)) {
            return;
        }
        String content = Files.readString(buildFile);
        List<String> starterTestLines = content.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();

        for (String line : starterTestLines) {
            assertThat(line.trim())
                    .as("spring-boot-starter-test in '%s' must use testImplementation scope — "
                            + "using implementation or api would leak test infrastructure "
                            + "into the production classpath", buildFilePath)
                    .startsWith("testImplementation(");
        }
    }

    @Test
    void configurationModule_shouldDeclareStarterTest() throws IOException {
        String configContent = Files.readString(
                projectRoot.resolve("configuration/build.gradle.kts"));
        assertThat(configContent)
                .as("configuration/build.gradle.kts must declare spring-boot-starter-test "
                        + "for @SpringBootTest integration tests")
                .contains("spring-boot-starter-test");
    }

    @Test
    void rootBuildFile_shouldNotDeclareStarterTest() throws IOException {
        String rootContent = Files.readString(projectRoot.resolve("build.gradle.kts"));
        assertThat(rootContent)
                .as("Root build.gradle.kts must NOT declare spring-boot-starter-test — "
                        + "it should not be applied globally via allprojects or subprojects")
                .doesNotContain("spring-boot-starter-test");
    }

}
