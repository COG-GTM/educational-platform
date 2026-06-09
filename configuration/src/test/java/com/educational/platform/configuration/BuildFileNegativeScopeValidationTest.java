package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Negative tests guarding against dependency scope drift in configuration/build.gradle.kts.
 * BuildDependencyScopeValidationTest verifies correct scopes; this test ensures that
 * incorrect scopes are NOT used. Scope drift (e.g., runtimeOnly or api for starter-test)
 * would either leak test infrastructure into production classpath or fail test compilation.
 */
class BuildFileNegativeScopeValidationTest {

    private static String buildContent;

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
    }

    @Test
    void starterTest_shouldNotBeRuntimeOnly() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("runtimeOnly")))
                .as("spring-boot-starter-test must NOT use runtimeOnly — "
                        + "test classes need compile-time access to @SpringBootTest, AssertJ, etc.")
                .isTrue();
    }

    @Test
    void starterTest_shouldNotBeApi() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("api(")))
                .as("spring-boot-starter-test must NOT use api scope — "
                        + "test dependencies should not be exposed to consuming modules")
                .isTrue();
    }

    @Test
    void starterTest_shouldNotBeCompileOnly() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("compileOnly")))
                .as("spring-boot-starter-test must NOT use compileOnly — "
                        + "test classes need runtime access for @SpringBootTest context loading")
                .isTrue();
    }

    @Test
    void starterWeb_shouldNotBeCompileOnly() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("compileOnly")))
                .as("spring-boot-starter-web must NOT use compileOnly — "
                        + "embedded Tomcat needs to be on runtime classpath for bootRun")
                .isTrue();
    }

    @Test
    void starterWeb_shouldNotBeTestImplementation() {
        assertThat(buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("testImplementation")))
                .as("spring-boot-starter-web must NOT be test-scoped — "
                        + "it is needed at runtime for bootRun HTTP serving")
                .isTrue();
    }

    @Test
    void buildFile_shouldNotContainAnnotationProcessorScope() {
        assertThat(buildContent)
                .as("annotationProcessor scope should not be present unless explicitly needed — "
                        + "the Spring Boot plugin does not require annotation processing")
                .doesNotContain("annotationProcessor(");
    }

    @Test
    void buildFile_shouldNotContainDevelopmentOnlyScope() {
        assertThat(buildContent)
                .as("developmentOnly scope should not be present — "
                        + "DevTools are not configured for this project")
                .doesNotContain("developmentOnly(");
    }
}
