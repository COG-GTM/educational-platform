package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that spring-boot-starter-test infrastructure does NOT bleed into the
 * production dependency graph. While scope isolation is validated at the build-file
 * level by {@link StarterTestModuleIsolationTest} and
 * {@link ConfigurationModuleDependencyScopeExclusivenessTest}, this test validates
 * the invariant from multiple angles:
 * <ul>
 *   <li>No test-only annotations reference in production source code</li>
 *   <li>No test-specific configuration properties in production resources</li>
 *   <li>No @SpringBootTest or @MockBean usage in main source set</li>
 * </ul>
 * Guards against a misconfigured build where testImplementation dependencies
 * accidentally become available in the production classpath.
 */
class StarterTestProductionClasspathIsolationTest {

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
    void productionSource_shouldNotImportSpringBootTest() throws IOException {
        Path mainJava = projectRoot.resolve("configuration/src/main/java");
        if (!Files.exists(mainJava)) return;

        List<Path> javaFiles;
        try (var stream = Files.walk(mainJava)) {
            javaFiles = stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }
        for (Path file : javaFiles) {
            String content = Files.readString(file);
            assertThat(content)
                    .as("Production source file '%s' must NOT import @SpringBootTest — "
                            + "test annotations belong only in test source set",
                            file.getFileName())
                    .doesNotContain("import org.springframework.boot.test.");
        }
    }

    @Test
    void productionSource_shouldNotImportMockito() throws IOException {
        Path mainJava = projectRoot.resolve("configuration/src/main/java");
        if (!Files.exists(mainJava)) return;

        List<Path> javaFiles;
        try (var stream = Files.walk(mainJava)) {
            javaFiles = stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }
        for (Path file : javaFiles) {
            String content = Files.readString(file);
            assertThat(content)
                    .as("Production source file '%s' must NOT import Mockito — "
                            + "mocking infrastructure belongs only in the test source set",
                            file.getFileName())
                    .doesNotContain("import org.mockito.");
        }
    }

    @Test
    void productionResources_shouldNotContainTestProperties() throws IOException {
        Path mainResources = projectRoot.resolve("configuration/src/main/resources");
        if (!Files.exists(mainResources)) return;

        List<Path> propertyFiles;
        try (var stream = Files.walk(mainResources)) {
            propertyFiles = stream
                    .filter(p -> p.toString().endsWith(".properties") || p.toString().endsWith(".yml"))
                    .toList();
        }
        for (Path file : propertyFiles) {
            String content = Files.readString(file);
            assertThat(content)
                    .as("Production resource '%s' must NOT contain test-specific properties",
                            file.getFileName())
                    .doesNotContain("spring.test.")
                    .doesNotContain("spring.autoconfigure.exclude");
        }
    }

    @Test
    void buildFile_starterTestDeclaration_shouldUseTestScope() throws IOException {
        String buildContent = Files.readString(
                projectRoot.resolve("configuration/build.gradle.kts"));
        List<String> starterTestLines = buildContent.lines()
                .filter(line -> line.contains("spring-boot-starter-test"))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        assertThat(starterTestLines)
                .as("spring-boot-starter-test must be declared at least once")
                .isNotEmpty();
        for (String line : starterTestLines) {
            assertThat(line.trim())
                    .as("Every spring-boot-starter-test declaration must use testImplementation")
                    .startsWith("testImplementation(");
        }
    }
}
