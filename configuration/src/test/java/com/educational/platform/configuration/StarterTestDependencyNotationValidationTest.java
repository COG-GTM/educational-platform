package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the declaration format and version management of the
 * spring-boot-starter-test dependency in configuration/build.gradle.kts.
 * The PR added this dependency as a two-argument (group, artifact)
 * testImplementation without an explicit version, relying on the BOM
 * imported by io.spring.dependency-management in the root build.
 * These tests guard against:
 * <ul>
 *   <li>Accidental addition of an explicit version that would bypass BOM management</li>
 *   <li>Exclusion blocks that strip key transitive deps (Mockito, AssertJ, JUnit)</li>
 *   <li>Incorrect artifact coordinates (typo in group or artifact name)</li>
 * </ul>
 */
class StarterTestDependencyNotationValidationTest {

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
    void starterTest_shouldUseTwoArgGroupArtifactNotation() {
        assertThat(buildContent)
                .as("spring-boot-starter-test must use two-argument (group, artifact) notation "
                        + "matching the project convention for Spring Boot starters")
                .containsPattern(
                        "testImplementation\\s*\\(\\s*\"org\\.springframework\\.boot\"\\s*,\\s*\"spring-boot-starter-test\"\\s*\\)");
    }

    @Test
    void starterTest_shouldNotSpecifyExplicitVersion() {
        List<String> starterTestLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(starterTestLines).isNotEmpty();
        for (String line : starterTestLines) {
            assertThat(line)
                    .as("spring-boot-starter-test must NOT have an explicit version — "
                            + "version is managed by the Spring Boot BOM via io.spring.dependency-management")
                    .doesNotContainPattern("spring-boot-starter-test\"\\s*,\\s*\"[\\d.]");
        }
    }

    @Test
    void starterTest_shouldNotHaveExclusionBlock() {
        String afterStarterTest = buildContent.substring(
                buildContent.indexOf("spring-boot-starter-test"));
        String nextLine = afterStarterTest.lines().skip(1).findFirst().orElse("");
        assertThat(nextLine.trim())
                .as("spring-boot-starter-test must not be followed by an exclude block — "
                        + "starter-test transitive deps (AssertJ, Mockito, Hamcrest, JSONassert) are all needed")
                .doesNotStartWith("exclude");
    }

    @Test
    void starterTest_shouldUseCorrectGroupId() {
        List<String> starterTestLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(starterTestLines).isNotEmpty();
        for (String line : starterTestLines) {
            assertThat(line)
                    .as("spring-boot-starter-test must use org.springframework.boot group ID")
                    .contains("org.springframework.boot");
        }
    }

    @Test
    void starterWeb_shouldAlsoUseNoExplicitVersion() {
        List<String> starterWebLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(starterWebLines).isNotEmpty();
        for (String line : starterWebLines) {
            assertThat(line)
                    .as("spring-boot-starter-web must NOT have an explicit version — "
                            + "consistency with starter-test: both should rely on BOM version management")
                    .doesNotContainPattern("spring-boot-starter-web\"\\s*,\\s*\"[\\d.]");
        }
    }

    @Test
    void starterTest_andStarterWeb_shouldUseSameGroupId() {
        String starterTestGroup = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .map(line -> line.replaceAll(".*\"(org\\.[^\"]+)\".*", "$1"))
                .findFirst().orElse("");
        String starterWebGroup = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .map(line -> line.replaceAll(".*\"(org\\.[^\"]+)\".*", "$1"))
                .findFirst().orElse("");
        assertThat(starterTestGroup)
                .as("starter-test and starter-web must use the same group ID "
                        + "to ensure consistent BOM resolution")
                .isEqualTo(starterWebGroup);
    }
}
