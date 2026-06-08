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

/**
 * Cross-validates the Spring Boot version across three layers in a single test
 * to catch version skew that individual tests might miss:
 * <ol>
 *   <li>gradle/libs.versions.toml declares the "spring" version key</li>
 *   <li>The springboot plugin in [plugins] references version.ref = "spring"</li>
 *   <li>The root build.gradle.kts BOM import uses libs.versions.spring.get()</li>
 *   <li>The runtime SpringBootVersion.getVersion() reflects the resolved version</li>
 * </ol>
 * Complements {@link VersionCatalogPluginVersionRefAlignmentTest} (TOML structure)
 * and {@link SpringBootPluginVersionTest} (runtime version alone) by verifying the
 * end-to-end version pipeline from TOML → BOM → runtime in one test class.
 */
class PluginBomRuntimeVersionCrossValidationTest {

    private static String tomlContent;
    private static String rootBuildContent;

    @BeforeAll
    static void loadBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
        rootBuildContent = Files.readString(dir.resolve("build.gradle.kts"));
    }

    @Test
    void tomlDeclaredVersion_shouldMatchRuntimeVersion() {
        String declaredVersion = extractTomlVersionValue("spring");
        String runtimeVersion = SpringBootVersion.getVersion();

        assertThat(runtimeVersion)
                .as("Runtime Spring Boot version (%s) must match the version declared "
                        + "in libs.versions.toml spring key (%s) — a mismatch indicates "
                        + "the BOM or plugin version resolution is broken",
                        runtimeVersion, declaredVersion)
                .isEqualTo(declaredVersion);
    }

    @Test
    void springbootPlugin_versionRef_shouldResolveToSameValueAsBom() {
        String pluginVersionRef = extractPluginVersionRef("springboot");
        assertThat(pluginVersionRef)
                .as("springboot plugin must have a version.ref")
                .isNotNull();

        String pluginResolvedVersion = extractTomlVersionValue(pluginVersionRef);
        String runtimeVersion = SpringBootVersion.getVersion();

        assertThat(pluginResolvedVersion)
                .as("The version.ref '%s' used by the springboot plugin must resolve to "
                        + "the same value as the runtime version — this confirms the "
                        + "plugin and BOM are using the exact same version key",
                        pluginVersionRef)
                .isEqualTo(runtimeVersion);
    }

    @Test
    void rootBuild_bomVersionRef_shouldAlignWithPluginVersionRef() {
        String pluginVersionRef = extractPluginVersionRef("springboot");
        assertThat(pluginVersionRef).isNotNull();

        assertThat(rootBuildContent)
                .as("Root build BOM import must use libs.versions.%s.get() — "
                        + "the same key referenced by the springboot plugin — "
                        + "to guarantee plugin and dependency version alignment",
                        pluginVersionRef)
                .containsPattern("libs\\.versions\\." + pluginVersionRef + "\\.get\\(\\)");
    }

    @Test
    void runtimeVersion_majorMinor_shouldMatchTomlDeclaration() {
        String declared = extractTomlVersionValue("spring");
        String runtime = SpringBootVersion.getVersion();

        String declaredMajorMinor = declared.substring(0, declared.lastIndexOf('.'));
        String runtimeMajorMinor = runtime.substring(0, runtime.lastIndexOf('.'));

        assertThat(runtimeMajorMinor)
                .as("Runtime major.minor (%s) must match TOML major.minor (%s)",
                        runtimeMajorMinor, declaredMajorMinor)
                .isEqualTo(declaredMajorMinor);
    }

    private String extractTomlVersionValue(String versionKey) {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(versionsIdx, pluginsIdx);
        for (String line : versionsSection.lines().toList()) {
            if (line.trim().startsWith(versionKey + " ") || line.trim().startsWith(versionKey + "=")) {
                Matcher m = Pattern.compile("=\\s*\"([^\"]+)\"").matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }

    private String extractPluginVersionRef(String pluginName) {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String pluginsSection = tomlContent.substring(pluginsIdx);
        for (String line : pluginsSection.lines().toList()) {
            if (line.trim().startsWith(pluginName + " ") || line.trim().startsWith(pluginName + "=")) {
                Matcher m = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"").matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            }
        }
        return null;
    }
}
