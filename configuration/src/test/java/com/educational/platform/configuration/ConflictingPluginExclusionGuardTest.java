package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against Gradle plugins that conflict with the Spring Boot plugin
 * being applied in the configuration module. Existing tests cover the
 * {@code application} plugin (in {@link BootPluginJarTaskConflictTest}) and
 * the root build exclusion (in {@link RootBuildFileSpringBootPluginExclusionTest}).
 * This test covers additional conflicting plugins:
 * <ul>
 *   <li>{@code war} — replaces JAR packaging with WAR; conflicts with bootJar
 *       and forces a servlet container deployment model</li>
 *   <li>{@code com.github.johnrengelman.shadow} — produces a shadow JAR that
 *       conflicts with the fat JAR produced by bootJar</li>
 *   <li>{@code org.graalvm.buildtools.native} — registers native-image tasks
 *       that require additional build infrastructure not present in this project</li>
 *   <li>{@code kotlin("jvm")} — the project is pure Java; applying the Kotlin
 *       plugin would add unnecessary compilation steps and classpath entries</li>
 * </ul>
 */
class ConflictingPluginExclusionGuardTest {

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
    void buildFile_shouldNotApplyWarPlugin() {
        assertThat(buildContent)
                .as("The 'war' plugin must NOT be applied — it replaces jar packaging "
                        + "with WAR and conflicts with bootJar. The configuration module "
                        + "produces a standalone executable JAR via the Spring Boot plugin.")
                .doesNotContain("\"war\"")
                .doesNotContainPattern("\\bwar\\b");
    }

    @Test
    void buildFile_shouldNotApplyShadowPlugin() {
        assertThat(buildContent)
                .as("Shadow (fat JAR) plugin must NOT be applied — the Spring Boot plugin "
                        + "already produces a fat JAR via bootJar; shadow would create a "
                        + "conflicting artifact with incompatible class loading")
                .doesNotContain("com.github.johnrengelman.shadow")
                .doesNotContain("shadow");
    }

    @Test
    void buildFile_shouldNotApplyNativeImagePlugin() {
        assertThat(buildContent)
                .as("GraalVM native-image plugin must NOT be applied — the project "
                        + "uses standard JVM deployment; native-image requires additional "
                        + "metadata and build infrastructure not configured here")
                .doesNotContain("org.graalvm.buildtools.native")
                .doesNotContain("native-image");
    }

    @Test
    void buildFile_shouldNotApplyKotlinPlugin() {
        assertThat(buildContent)
                .as("Kotlin JVM plugin must NOT be applied — the configuration module "
                        + "is pure Java; adding Kotlin would introduce unnecessary "
                        + "compilation overhead and classpath entries")
                .doesNotContainPattern("kotlin\\s*\\(\\s*\"jvm\"\\s*\\)")
                .doesNotContain("org.jetbrains.kotlin");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ear",
            "scala",
            "antlr",
            "org.springframework.experimental.aot"
    })
    void buildFile_shouldNotApplyUnexpectedPlugin(String pluginId) {
        assertThat(buildContent)
                .as("Plugin '%s' must NOT be applied in the configuration module — "
                        + "only the Spring Boot plugin is expected in the plugins block", pluginId)
                .doesNotContain(pluginId);
    }

    @Test
    void buildFile_shouldNotApplySpringCloudPlugin() {
        assertThat(buildContent)
                .as("Spring Cloud plugin must NOT be applied in the configuration module — "
                        + "cloud-native features require separate infrastructure setup "
                        + "and would add BOM management conflicts with the Spring Boot BOM")
                .doesNotContain("spring-cloud");
    }
}
