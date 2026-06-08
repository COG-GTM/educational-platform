package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the coexistence of the two Spring-related Gradle plugins:
 * <ul>
 *   <li>{@code io.spring.dependency-management} — applied at root level in
 *       build.gradle.kts for BOM-based version management across all subprojects</li>
 *   <li>{@code org.springframework.boot} — applied only in configuration/build.gradle.kts
 *       for bootRun/bootJar task registration</li>
 * </ul>
 * Both plugins must coexist without conflict. The dependency-management plugin
 * imports the BOM for transitive dependency version control, while the Spring Boot
 * plugin registers boot tasks and auto-detects the main class. If either is removed
 * or applied at the wrong level, the build will fail.
 */
class PluginCoexistenceValidationTest {

    private static Path projectRoot;
    private static String rootBuildContent;
    private static String configBuildContent;
    private static String tomlContent;

    @BeforeAll
    static void loadBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        rootBuildContent = Files.readString(projectRoot.resolve("build.gradle.kts"));
        configBuildContent = Files.readString(projectRoot.resolve("configuration/build.gradle.kts"));
        tomlContent = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void springDependencyManagement_shouldBeAtRootLevel() {
        assertThat(rootBuildContent)
                .as("io.spring.dependency-management must be applied at root level (build.gradle.kts)")
                .contains("libs.plugins.springdependencies");
    }

    @Test
    void springBootPlugin_shouldBeAtModuleLevel() {
        assertThat(configBuildContent)
                .as("org.springframework.boot must be applied at module level (configuration/build.gradle.kts)")
                .contains("libs.plugins.springboot");
    }

    @Test
    void springDependencyManagement_shouldNotBeInConfigurationModule() {
        assertThat(configBuildContent)
                .as("configuration module must NOT re-apply the dependency-management plugin — "
                        + "it inherits from root's subprojects block")
                .doesNotContain("springdependencies")
                .doesNotContain("io.spring.dependency-management");
    }

    @Test
    void springBootPlugin_shouldNotBeAppliedAtRootLevel() {
        assertThat(rootBuildContent)
                .as("Spring Boot plugin must NOT be applied at root level — only the configuration module needs boot tasks")
                .doesNotContain("libs.plugins.springboot");
        // Verify the plugin ID is not used in an apply/plugin context (BOM coordinate reference is fine)
        assertThat(rootBuildContent.lines()
                .filter(line -> line.contains("plugin") && line.contains("org.springframework.boot"))
                .filter(line -> !line.trim().startsWith("//"))
                .filter(line -> !line.contains("dependencies"))
                .toList())
                .as("Root build must NOT apply org.springframework.boot as a plugin")
                .isEmpty();
    }

    @Test
    void bothPlugins_shouldBeDeclaredInVersionCatalog() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);

        assertThat(pluginsSection)
                .as("Both plugins must be declared in the version catalog for centralized management")
                .contains("springdependencies")
                .contains("springboot");
    }

    @Test
    void dependencyManagementBom_shouldResolveCorrectVersionAtRuntime() {
        String runtimeVersion = SpringBootVersion.getVersion();

        // Extract declared version from TOML
        Matcher m = Pattern.compile("^spring\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE)
                .matcher(tomlContent);
        assertThat(m.find()).isTrue();
        String declaredVersion = m.group(1);

        assertThat(runtimeVersion)
                .as("Runtime version (managed by dependency-management BOM) must match the "
                        + "version declared in TOML (used by both plugins)")
                .isEqualTo(declaredVersion);
    }

    @Test
    void springBootAutoConfigure_shouldBeAvailable_viaBomManagement() {
        assertThatCode(() -> Class.forName("org.springframework.boot.autoconfigure.EnableAutoConfiguration"))
                .as("EnableAutoConfiguration must be resolvable — confirms BOM manages spring-boot-autoconfigure "
                        + "and the Spring Boot plugin can trigger auto-configuration")
                .doesNotThrowAnyException();
    }

    @Test
    void rootSubprojectsBlock_shouldApplyDependencyManagementToAllModules() {
        assertThat(rootBuildContent)
                .as("Root build must apply dependency-management plugin in subprojects block")
                .containsPattern("subprojects\\s*\\{[\\s\\S]*io\\.spring\\.dependency-management");
    }

    @Test
    void rootSubprojectsBlock_shouldNotApplySpringBootPluginToAllModules() {
        String subprojectsBlock = extractSubprojectsBlock(rootBuildContent);
        // Check that the subprojects block doesn't apply the Spring Boot plugin
        // (it may contain org.springframework.boot as part of the BOM coordinate)
        assertThat(subprojectsBlock.lines()
                .filter(line -> line.contains("plugin"))
                .filter(line -> !line.trim().startsWith("//"))
                .noneMatch(line -> line.contains("org.springframework.boot") || line.contains("springboot")))
                .as("Root subprojects block must NOT apply org.springframework.boot plugin — "
                        + "only the configuration module should have boot tasks")
                .isTrue();
    }

    private String extractSubprojectsBlock(String content) {
        int start = content.indexOf("subprojects");
        if (start < 0) return "";
        int braceStart = content.indexOf('{', start);
        if (braceStart < 0) return "";
        int depth = 0;
        for (int i = braceStart; i < content.length(); i++) {
            if (content.charAt(i) == '{') depth++;
            else if (content.charAt(i) == '}') {
                depth--;
                if (depth == 0) return content.substring(braceStart, i + 1);
            }
        }
        return content.substring(braceStart);
    }
}
