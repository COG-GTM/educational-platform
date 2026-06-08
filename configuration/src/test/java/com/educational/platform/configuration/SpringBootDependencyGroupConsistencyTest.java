package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that all Spring Boot dependencies declared in
 * configuration/build.gradle.kts use the correct and consistent group ID
 * ({@code org.springframework.boot}). The PR added spring-boot-starter-test;
 * these tests ensure:
 * <ul>
 *   <li>All spring-boot-* artifacts use the canonical group ID</li>
 *   <li>No third-party repackaged Spring Boot starters are used</li>
 *   <li>The group ID is not accidentally misspelled (e.g., "org.springboot")</li>
 *   <li>Implementation and test scopes consistently reference the same group</li>
 * </ul>
 * Using an incorrect group ID would cause the dependency-management plugin
 * to NOT apply BOM version management, resulting in unresolved dependencies.
 */
class SpringBootDependencyGroupConsistencyTest {

    private static String buildContent;
    private static List<String> springBootDepLines;

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
        springBootDepLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter"))
                .filter(line -> !line.trim().startsWith("//"))
                .map(String::trim)
                .toList();
    }

    @Test
    void allSpringBootStarters_shouldUseCanonicalGroupId() {
        for (String line : springBootDepLines) {
            assertThat(line)
                    .as("Spring Boot starter dependency must use group 'org.springframework.boot': %s", line)
                    .contains("org.springframework.boot");
        }
    }

    @Test
    void implementationStarters_shouldUseImplementationScope() {
        List<String> implStarters = springBootDepLines.stream()
                .filter(line -> line.contains("spring-boot-starter-web"))
                .toList();
        for (String line : implStarters) {
            assertThat(line)
                    .as("spring-boot-starter-web must use implementation scope (not api or runtimeOnly)")
                    .startsWith("implementation(");
        }
    }

    @Test
    void testStarters_shouldUseTestImplementationScope() {
        List<String> testStarters = springBootDepLines.stream()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .toList();
        for (String line : testStarters) {
            assertThat(line)
                    .as("spring-boot-starter-test must use testImplementation scope")
                    .startsWith("testImplementation(");
        }
    }

    @Test
    void buildFile_shouldNotReferenceThirdPartySpringBootGroupIds() {
        assertThat(buildContent)
                .as("Build file must NOT reference third-party repackaged Spring Boot group IDs "
                        + "— only org.springframework.boot is the canonical group")
                .doesNotContain("com.github.spring-boot")
                .doesNotContain("io.springboot")
                .doesNotContain("org.springboot");
    }

    @Test
    void springBootStarters_shouldNotSpecifyExplicitVersions() {
        for (String line : springBootDepLines) {
            long commaCount = line.chars().filter(c -> c == ',').count();
            assertThat(commaCount)
                    .as("Spring Boot starter '%s' must use (group, artifact) two-arg form "
                            + "without explicit version — version is managed by BOM", line)
                    .isEqualTo(1);
        }
    }

    @Test
    void buildFile_shouldNotMixStringNotationForSpringBootDeps() {
        List<String> stringNotationLines = buildContent.lines()
                .filter(line -> line.contains("org.springframework.boot:spring-boot-starter"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(stringNotationLines)
                .as("Spring Boot starters must NOT use single-string 'group:artifact:version' notation — "
                        + "the project convention is two-argument (group, artifact) form for consistency")
                .isEmpty();
    }
}
