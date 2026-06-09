package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Spring Boot starter declarations in configuration/build.gradle.kts
 * are complete and correctly structured for bootRun to produce a working HTTP
 * application. The Spring Boot plugin's bootRun task starts an embedded web
 * server — without starter-web on the implementation classpath, bootRun would
 * start but immediately shut down (no embedded server detected). Without
 * starter-test on the testImplementation classpath, @SpringBootTest would
 * fail to load the application context in test mode.
 * <p>
 * Complements {@link BootRunTaskRequirementsValidationTest} which validates
 * plugin/main-class prerequisites, and {@link BuildDependencyScopeValidationTest}
 * which validates individual scope assignments.
 */
class SpringBootStarterWebBootRunRequirementsTest {

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
    void starterWeb_shouldBeImplementationScope() {
        List<String> starterWebLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.startsWith("//"))
                .toList();
        assertThat(starterWebLines)
                .as("spring-boot-starter-web must be declared in build file")
                .isNotEmpty();
        assertThat(starterWebLines.getFirst())
                .as("spring-boot-starter-web must use implementation scope — "
                        + "the embedded web server must be on the runtime classpath for bootRun")
                .startsWith("implementation(");
    }

    @Test
    void starterWeb_shouldUseTwoArgNotation() {
        assertThat(buildContent)
                .as("spring-boot-starter-web must use two-argument (group, artifact) notation "
                        + "consistent with starter-test")
                .containsPattern(
                        "implementation\\s*\\(\\s*\"org\\.springframework\\.boot\"\\s*,\\s*\"spring-boot-starter-web\"\\s*\\)");
    }

    @Test
    void starterWeb_shouldNotHaveExplicitVersion() {
        List<String> webLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        for (String line : webLines) {
            assertThat(line)
                    .as("spring-boot-starter-web must NOT specify an explicit version — "
                            + "the version is managed by the BOM via io.spring.dependency-management")
                    .doesNotContainPattern("spring-boot-starter-web\"\\s*,\\s*\"[\\d.]");
        }
    }

    @Test
    void starterWeb_shouldAppearBeforeStarterTest() {
        int webIdx = buildContent.indexOf("spring-boot-starter-web");
        int testIdx = buildContent.indexOf("spring-boot-starter-test");
        assertThat(webIdx)
                .as("spring-boot-starter-web must appear before spring-boot-starter-test — "
                        + "runtime dependencies are conventionally listed before test dependencies")
                .isLessThan(testIdx);
    }

    @Test
    void buildFile_shouldNotDeclareStarterParent() {
        assertThat(buildContent)
                .as("spring-boot-starter-parent must NOT be declared — "
                        + "the project uses BOM import via io.spring.dependency-management "
                        + "in the root build, not the starter-parent POM")
                .doesNotContain("spring-boot-starter-parent");
    }

    @Test
    void buildFile_shouldDeclareBothStarters() {
        assertThat(buildContent)
                .as("Both spring-boot-starter-web (implementation) and "
                        + "spring-boot-starter-test (testImplementation) must be present")
                .contains("spring-boot-starter-web")
                .contains("spring-boot-starter-test");
    }

    @Test
    void buildFile_starterCount_shouldBeExactlyTwo() {
        long starterCount = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-"))
                .filter(line -> !line.trim().startsWith("//"))
                .count();
        assertThat(starterCount)
                .as("Configuration module must declare exactly 2 Spring Boot starters "
                        + "(starter-web + starter-test) — additional starters should "
                        + "be declared in their respective domain modules")
                .isEqualTo(2);
    }
}
