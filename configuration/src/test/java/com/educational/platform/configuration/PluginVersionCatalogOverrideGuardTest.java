package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against plugin version overrides in configuration/build.gradle.kts.
 * When using the Gradle version catalog with {@code alias(libs.plugins.springboot)},
 * appending {@code .version("X")} or using {@code apply false} after the alias
 * would override the centrally managed version from libs.versions.toml. This creates
 * a divergence between the plugin version and the BOM version (both should derive
 * from the same "spring" key), leading to classpath conflicts at runtime.
 * <p>
 * Also validates that no inline ID syntax or buildscript classpath overrides are used,
 * which would similarly bypass version catalog management.
 */
class PluginVersionCatalogOverrideGuardTest {

    private static String buildContent;
    private static List<String> buildLines;

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
        buildLines = buildContent.lines().toList();
    }

    @Test
    void pluginAlias_shouldNotHaveVersionOverride() {
        List<String> aliasLines = buildLines.stream()
                .filter(line -> line.contains("alias("))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        for (String line : aliasLines) {
            assertThat(line)
                    .as("Plugin alias must NOT call .version() — the version is managed "
                            + "exclusively by the version catalog (libs.versions.toml). "
                            + "Overriding would break BOM alignment.")
                    .doesNotContainPattern("\\.version\\s*\\(");
        }
    }

    @Test
    void pluginAlias_shouldNotHaveApplyFalseChain() {
        List<String> aliasLines = buildLines.stream()
                .filter(line -> line.contains("alias("))
                .filter(line -> !line.trim().startsWith("//"))
                .toList();
        for (String line : aliasLines) {
            assertThat(line)
                    .as("Plugin alias must NOT chain .apply(false) — the Spring Boot plugin "
                            + "must be actively applied to register bootRun/bootJar tasks")
                    .doesNotContainPattern("\\.apply\\s*\\(\\s*false\\s*\\)");
        }
    }

    @Test
    void buildFile_shouldNotUseBuildscriptClasspath() {
        assertThat(buildContent)
                .as("configuration module must NOT use buildscript { classpath(...) } — "
                        + "plugins must be applied via the version catalog alias() DSL, "
                        + "not the legacy buildscript classpath mechanism")
                .doesNotContain("classpath(");
    }

    @Test
    void buildFile_shouldNotUseIdSyntaxForAnyPlugin() {
        int pluginsStart = buildContent.indexOf("plugins");
        int pluginsEnd = buildContent.indexOf("}", pluginsStart);
        if (pluginsStart < 0 || pluginsEnd < 0) return;
        String pluginsBlock = buildContent.substring(pluginsStart, pluginsEnd + 1);
        assertThat(pluginsBlock)
                .as("plugins block must only use alias() syntax — id() bypasses "
                        + "version catalog management and makes version coordination harder")
                .doesNotContainPattern("\\bid\\s*\\(");
    }

    @Test
    void buildFile_shouldNotUseKotlinDslApplyPlugin() {
        assertThat(buildContent)
                .as("configuration module must NOT use apply(plugin = ...) or apply { plugin(...) } — "
                        + "these legacy mechanisms bypass version catalog alignment")
                .doesNotContainPattern("apply\\s*\\(\\s*plugin")
                .doesNotContainPattern("apply\\s*\\{[^}]*plugin\\(");
    }
}
