package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the exact set of plugins applied in the root build.gradle.kts
 * {@code subprojects { apply { ... } }} block. The configuration module
 * inherits these plugins — removing any of them would break the build:
 * <ul>
 *   <li>{@code java} — required for compilation tasks</li>
 *   <li>{@code io.spring.dependency-management} — required for BOM version
 *       resolution of Spring Boot starters declared without explicit versions</li>
 *   <li>{@code java-library} — required for the {@code api} / {@code implementation}
 *       scope distinction used by library modules</li>
 * </ul>
 * Complements {@link PluginCoexistenceValidationTest} which validates plugin
 * coexistence across root and configuration module, but does not assert the
 * exact plugin set inside the subprojects apply block.
 */
class RootBuildSubprojectsPluginSetTest {

    private static String rootBuildContent;

    @BeforeAll
    static void loadRootBuildFile() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        rootBuildContent = Files.readString(dir.resolve("build.gradle.kts"));
    }

    @Test
    void subprojects_shouldApplyJavaPlugin() {
        assertThat(rootBuildContent)
                .as("subprojects apply block must include 'java' plugin — "
                        + "all modules need Java compilation tasks")
                .containsPattern("plugin\\s*\\(\\s*\"java\"\\s*\\)");
    }

    @Test
    void subprojects_shouldApplyDependencyManagementPlugin() {
        assertThat(rootBuildContent)
                .as("subprojects apply block must include 'io.spring.dependency-management' — "
                        + "required for BOM-based version resolution of Spring Boot starters "
                        + "declared without explicit versions in configuration/build.gradle.kts")
                .containsPattern(
                        "plugin\\s*\\(\\s*\"io\\.spring\\.dependency-management\"\\s*\\)");
    }

    @Test
    void subprojects_shouldApplyJavaLibraryPlugin() {
        assertThat(rootBuildContent)
                .as("subprojects apply block must include 'java-library' — "
                        + "required for the api/implementation scope distinction "
                        + "used by library modules")
                .containsPattern("plugin\\s*\\(\\s*\"java-library\"\\s*\\)");
    }

    @Test
    void subprojects_applyBlock_shouldContainExactlyThreePlugins() {
        int subprojectsIdx = rootBuildContent.indexOf("subprojects");
        assertThat(subprojectsIdx).isGreaterThanOrEqualTo(0);
        String subprojectsBlock = rootBuildContent.substring(subprojectsIdx);

        Matcher applyMatcher = Pattern.compile("apply\\s*\\{([^}]*)}", Pattern.DOTALL)
                .matcher(subprojectsBlock);
        assertThat(applyMatcher.find())
                .as("subprojects block must contain an apply {} block")
                .isTrue();
        String applyContent = applyMatcher.group(1);

        List<String> pluginLines = applyContent.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("plugin("))
                .toList();
        assertThat(pluginLines)
                .as("subprojects apply block must contain exactly 3 plugin applications "
                        + "(java, io.spring.dependency-management, java-library) — "
                        + "adding more plugins here would affect every module including "
                        + "the configuration module with the Spring Boot plugin")
                .hasSize(3);
    }

    @Test
    void subprojects_shouldNotApplySpringBootPluginViaApply() {
        int subprojectsIdx = rootBuildContent.indexOf("subprojects");
        assertThat(subprojectsIdx).isGreaterThanOrEqualTo(0);

        Matcher applyMatcher = Pattern.compile("apply\\s*\\{([^}]*)}", Pattern.DOTALL)
                .matcher(rootBuildContent.substring(subprojectsIdx));
        assertThat(applyMatcher.find())
                .as("subprojects block must contain an apply {} block")
                .isTrue();
        String applyContent = applyMatcher.group(1);
        assertThat(applyContent)
                .as("subprojects apply block must NOT apply org.springframework.boot — "
                        + "only the configuration module applies it via its own plugins block")
                .doesNotContain("org.springframework.boot");
    }

    @Test
    void subprojects_shouldContainDependencyManagementBlock() {
        int subprojectsIdx = rootBuildContent.indexOf("subprojects");
        assertThat(subprojectsIdx).isGreaterThanOrEqualTo(0);
        String subprojectsBlock = rootBuildContent.substring(subprojectsIdx);
        assertThat(subprojectsBlock)
                .as("subprojects block must contain a dependencyManagement block — "
                        + "the BOM import is required for Spring Boot starter version resolution")
                .contains("dependencyManagement");
    }
}
