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
 * Validates the exact plugin set in the root build.gradle.kts plugins block.
 * {@link RootBuildFileSpringBootPluginExclusionTest} guards against springboot
 * appearing at the root level; this test guards the positive invariant — the
 * root plugins block must contain exactly {@code java} and
 * {@code libs.plugins.springdependencies}. Adding unexpected plugins at root
 * (e.g., {@code application}, {@code kotlin("jvm")}, {@code war}) would affect
 * all submodules via Gradle's plugin inheritance and could interfere with the
 * Spring Boot plugin applied in the configuration module.
 */
class RootBuildPluginsBlockInvariantTest {

    private static String rootBuildContent;
    private static String pluginsBlockContent;

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

        Matcher m = Pattern.compile("plugins\\s*\\{([^}]*)}", Pattern.DOTALL)
                .matcher(rootBuildContent);
        assertThat(m.find())
                .as("Root build.gradle.kts must have a plugins {} block")
                .isTrue();
        pluginsBlockContent = m.group(1);
    }

    @Test
    void rootPluginsBlock_shouldContainJavaPlugin() {
        assertThat(pluginsBlockContent)
                .as("Root plugins block must include the 'java' plugin — "
                        + "it provides the Java compilation tasks inherited by all submodules")
                .containsPattern("\\bjava\\b");
    }

    @Test
    void rootPluginsBlock_shouldContainSpringDependencyManagementPlugin() {
        assertThat(pluginsBlockContent)
                .as("Root plugins block must include springdependencies — "
                        + "it enables BOM-based version management for all submodules")
                .contains("libs.plugins.springdependencies");
    }

    @Test
    void rootPluginsBlock_shouldContainExactlyTwoPluginDeclarations() {
        List<String> pluginLines = pluginsBlockContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("//"))
                .toList();
        assertThat(pluginLines)
                .as("Root plugins block must contain exactly 2 declarations "
                        + "(java and springdependencies) — additional plugins "
                        + "would affect all submodules and risk conflicts with "
                        + "the Spring Boot plugin in the configuration module")
                .hasSize(2);
    }

    @Test
    void rootPluginsBlock_shouldNotContainApplicationPlugin() {
        assertThat(pluginsBlockContent)
                .as("Root must NOT apply the 'application' plugin — it conflicts with "
                        + "Spring Boot's bootRun task and main class configuration")
                .doesNotContainPattern("\\bapplication\\b");
    }

    @Test
    void rootPluginsBlock_shouldNotContainWarPlugin() {
        assertThat(pluginsBlockContent)
                .as("Root must NOT apply the 'war' plugin — "
                        + "the project uses embedded Tomcat via Spring Boot, not WAR packaging")
                .doesNotContainPattern("\\bwar\\b");
    }

    @Test
    void rootPluginsBlock_shouldNotContainKotlinPlugin() {
        assertThat(pluginsBlockContent)
                .as("Root must NOT apply Kotlin plugins — "
                        + "the project is pure Java")
                .doesNotContain("kotlin");
    }
}
