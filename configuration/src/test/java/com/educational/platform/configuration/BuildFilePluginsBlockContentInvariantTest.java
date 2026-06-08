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
 * Validates the exact content of the plugins block in configuration/build.gradle.kts.
 * The PR added {@code alias(libs.plugins.springboot)} as the sole plugin. Existing
 * tests validate that the plugin is present and uses alias() syntax
 * ({@link PluginApplicationSyntaxValidationTest}), that it is not applied with
 * apply false ({@link BootJarConfigurationValidationTest}), and that it coexists
 * correctly with the dependency-management plugin at root level
 * ({@link PluginCoexistenceValidationTest}). This test guards the structural
 * invariant: the plugins block must contain exactly the springboot plugin
 * and nothing else — no java, no java-library (inherited from root), no
 * spring-dependency-management (applied at root level).
 */
class BuildFilePluginsBlockContentInvariantTest {

    private static String buildContent;
    private static String pluginsBlockContent;

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
        pluginsBlockContent = extractPluginsBlock(buildContent);
    }

    @Test
    void pluginsBlock_shouldExist() {
        assertThat(pluginsBlockContent)
                .as("configuration/build.gradle.kts must have a plugins {} block "
                        + "to apply the Spring Boot plugin")
                .isNotEmpty();
    }

    @Test
    void pluginsBlock_shouldContainOnlySpringBootPlugin() {
        List<String> pluginLines = pluginsBlockContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.equals("plugins {") && !line.equals("{") && !line.equals("}"))
                .filter(line -> !line.startsWith("//"))
                .toList();
        assertThat(pluginLines)
                .as("plugins block must contain exactly one entry: alias(libs.plugins.springboot)")
                .hasSize(1);
        assertThat(pluginLines.getFirst())
                .contains("libs.plugins.springboot");
    }

    @Test
    void pluginsBlock_shouldNotApplyJavaPlugin() {
        assertThat(pluginsBlockContent)
                .as("configuration module must NOT apply the java plugin — "
                        + "it is inherited from root build's subprojects block")
                .doesNotContainPattern("\\bjava\\b(?!\\.)");
    }

    @Test
    void pluginsBlock_shouldNotApplyJavaLibraryPlugin() {
        assertThat(pluginsBlockContent)
                .as("configuration module must NOT apply java-library — "
                        + "it is inherited from root build's subprojects block")
                .doesNotContain("java-library");
    }

    @Test
    void pluginsBlock_shouldNotApplyDependencyManagementPlugin() {
        assertThat(pluginsBlockContent)
                .as("configuration module must NOT apply io.spring.dependency-management — "
                        + "it is inherited from root build's subprojects block")
                .doesNotContain("springdependencies")
                .doesNotContain("dependency-management");
    }

    @Test
    void pluginsBlock_shouldBeTheFirstBlockInFile() {
        int pluginsStart = buildContent.indexOf("plugins");
        assertThat(pluginsStart)
                .as("plugins block must start at the very beginning of the file")
                .isEqualTo(0);
    }

    private static String extractPluginsBlock(String content) {
        Matcher m = Pattern.compile("plugins\\s*\\{", Pattern.MULTILINE).matcher(content);
        if (!m.find()) return "";
        int start = m.start();
        int depth = 0;
        for (int i = m.end() - 1; i < content.length(); i++) {
            if (content.charAt(i) == '{') depth++;
            else if (content.charAt(i) == '}') {
                depth--;
                if (depth == 0) return content.substring(start, i + 1);
            }
        }
        return content.substring(start);
    }
}
