package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that critical dependency declarations in configuration/build.gradle.kts
 * are not duplicated. Duplicate testImplementation entries for the same artifact
 * are a code smell and can cause version conflicts if one specifies an explicit
 * version while the other relies on BOM management. This test guards against
 * accidental duplicate additions during merge conflict resolution or copy-paste.
 */
class BuildFileStarterTestDeclarationUniquenessTest {

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
    void starterTest_shouldBeDeclaredExactlyOnce() {
        long count = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("spring-boot-starter-test must be declared exactly once — "
                        + "duplicates could cause version conflicts or scope confusion")
                .isEqualTo(1);
    }

    @Test
    void starterWeb_shouldBeDeclaredExactlyOnce() {
        long count = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("spring-boot-starter-web must be declared exactly once — "
                        + "duplicates would indicate a merge conflict regression")
                .isEqualTo(1);
    }

    @Test
    void springBootPlugin_shouldBeAppliedExactlyOnce() {
        long count = buildContent.lines()
                .filter(line -> line.contains("springboot") || line.contains("org.springframework.boot"))
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> line.contains("alias(") || line.contains("id("))
                .count();
        assertThat(count)
                .as("The Spring Boot plugin must be applied exactly once in the plugins block")
                .isEqualTo(1);
    }

    @Test
    void junitJupiterApi_shouldBeDeclaredExactlyOnce() {
        long count = buildContent.lines()
                .filter(line -> line.contains("junit-jupiter-api"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("junit-jupiter-api must be declared exactly once")
                .isEqualTo(1);
    }

    @Test
    void mockitoJunitJupiter_shouldBeDeclaredExactlyOnce() {
        long count = buildContent.lines()
                .filter(line -> line.contains("mockito-junit-jupiter"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("mockito-junit-jupiter must be declared exactly once")
                .isEqualTo(1);
    }

    @Test
    void archunitJunit5_shouldBeDeclaredExactlyOnce() {
        long count = buildContent.lines()
                .filter(line -> line.contains("archunit-junit5"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(count)
                .as("archunit-junit5 must be declared exactly once")
                .isEqualTo(1);
    }

    @Test
    void allTestDependencies_shouldNotHaveDuplicateArtifacts() {
        List<String> testDeps = buildContent.lines()
                .filter(line -> line.trim().startsWith("testImplementation("))
                .filter(line -> !line.trim().startsWith("//"))
                .map(String::trim)
                .toList();
        assertThat(testDeps)
                .as("All testImplementation declarations must be unique — "
                        + "no duplicate artifact lines should exist")
                .doesNotHaveDuplicates();
    }
}
