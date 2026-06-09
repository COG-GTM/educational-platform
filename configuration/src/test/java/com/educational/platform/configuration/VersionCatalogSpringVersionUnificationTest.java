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
 * Validates that the Spring Boot plugin version and the Spring Boot BOM
 * version are derived from the same version key in gradle/libs.versions.toml.
 * The PR added the springboot plugin entry pointing to {@code version.ref = "spring"},
 * and the root build.gradle.kts imports the BOM also using the "spring"
 * version. If these references diverge (e.g., one uses "spring" and another
 * uses a hardcoded version), classpath conflicts will occur between the
 * plugin's auto-configured dependencies and the BOM-managed versions.
 * <p>
 * Complements {@link PluginBomRuntimeVersionCrossValidationTest} (runtime
 * version alignment), {@link VersionCatalogPluginVersionRefAlignmentTest}
 * (structural ref alignment), and {@link SpringBootPluginVersionTest}
 * (exact version assertions).
 */
class VersionCatalogSpringVersionUnificationTest {

    private static String tomlContent;
    private static String rootBuildContent;

    @BeforeAll
    static void loadFiles() throws IOException {
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
    void springbootPluginVersionRef_shouldPointToSpringKey() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);
        String springbootLine = pluginsSection.lines()
                .filter(line -> line.trim().startsWith("springboot"))
                .findFirst()
                .orElse("");
        Matcher matcher = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"")
                .matcher(springbootLine);
        assertThat(matcher.find())
                .as("springboot plugin entry must have a version.ref declaration")
                .isTrue();
        assertThat(matcher.group(1))
                .as("springboot plugin version.ref must point to the 'spring' key — "
                        + "this ensures the plugin version and BOM version are identical")
                .isEqualTo("spring");
    }

    @Test
    void rootBuildBom_shouldReferenceTheSameSpringVersionKey() {
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts BOM import must reference libs.versions.spring — "
                        + "using a different version key would cause plugin/BOM version divergence")
                .containsPattern("spring-boot-dependencies.*libs\\.versions\\.spring\\.get\\(\\)");
    }

    @Test
    void springVersionInCatalog_shouldMatchRuntimeVersion() {
        Pattern versionPattern = Pattern.compile(
                "spring\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = versionPattern.matcher(tomlContent);
        assertThat(matcher.find())
                .as("The 'spring' version key must exist in [versions]")
                .isTrue();
        String catalogVersion = matcher.group(1);
        String runtimeVersion = SpringBootVersion.getVersion();
        assertThat(runtimeVersion)
                .as("Runtime Spring Boot version must match the version declared in "
                        + "libs.versions.toml [versions] spring key — divergence indicates "
                        + "a version override bypassing the catalog")
                .isEqualTo(catalogVersion);
    }

    @Test
    void springVersionKey_shouldBeTheSingleSourceOfTruth() {
        // Count references to the "spring" version.ref in the TOML
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);

        long springRefCount = pluginsSection.lines()
                .filter(line -> line.contains("version.ref") && line.contains("\"spring\""))
                .count();
        assertThat(springRefCount)
                .as("Only one plugin entry should reference the 'spring' version key — "
                        + "multiple plugins sharing a version ref that is Spring-Boot-specific "
                        + "could cause unintended version coupling")
                .isEqualTo(1);
    }
}
