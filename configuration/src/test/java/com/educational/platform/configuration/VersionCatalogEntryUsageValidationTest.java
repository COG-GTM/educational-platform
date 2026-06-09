package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates referential integrity of version entries in gradle/libs.versions.toml.
 * Version keys are referenced in two ways:
 * <ul>
 *   <li>Via {@code version.ref = "key"} in plugin entries within the TOML itself</li>
 *   <li>Via {@code libs.versions.key.get()} in Gradle build scripts (Kotlin DSL)</li>
 * </ul>
 * A version entry that is not referenced anywhere is stale and should be removed
 * to avoid confusion about which version key controls the Spring Boot ecosystem.
 */
class VersionCatalogEntryUsageValidationTest {

    private static Path projectRoot;
    private static String tomlContent;

    @BeforeAll
    static void loadVersionCatalog() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void allVersionKeys_shouldBeReferencedSomewhere() throws IOException {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(versionsIdx).isGreaterThanOrEqualTo(0);
        assertThat(pluginsIdx).isGreaterThan(versionsIdx);

        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);
        List<String> versionKeys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();

        String pluginsSection = tomlContent.substring(pluginsIdx);

        // Collect all build.gradle.kts content for reference checks
        StringBuilder allBuildContent = new StringBuilder();
        try (Stream<Path> paths = Files.walk(projectRoot)) {
            paths.filter(p -> p.getFileName().toString().equals("build.gradle.kts"))
                    .forEach(p -> {
                        try {
                            allBuildContent.append(Files.readString(p));
                        } catch (IOException ignored) {
                        }
                    });
        }
        String buildContent = allBuildContent.toString();

        for (String key : versionKeys) {
            // Convert camelCase key to dot-separated for Gradle accessor (e.g., springDependencyManagementPlugin -> springDependencyManagementPlugin)
            boolean referencedInToml = pluginsSection.contains("\"" + key + "\"");
            boolean referencedInBuild = buildContent.contains("libs.versions." + key + ".get()");
            // Also check for camelCase-to-dot conversion (Gradle convention)
            String dotKey = key.replaceAll("([A-Z])", ".$1").toLowerCase();
            boolean referencedInBuildDotted = buildContent.contains("libs.versions." + dotKey + ".get()");

            assertThat(referencedInToml || referencedInBuild || referencedInBuildDotted)
                    .as("Version key '%s' must be referenced by a plugin entry (version.ref) "
                            + "or a build script (libs.versions.%s.get()) — "
                            + "unused version keys should be removed to avoid confusion", key, key)
                    .isTrue();
        }
    }

    @Test
    void allPluginVersionRefs_shouldReferenceExistingVersionKeys() {
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);

        int versionsIdx = tomlContent.indexOf("[versions]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);

        Matcher matcher = Pattern.compile("version\\.ref\\s*=\\s*\"(\\w+)\"")
                .matcher(pluginsSection);

        while (matcher.find()) {
            String ref = matcher.group(1);
            assertThat(versionsSection)
                    .as("Plugin version.ref '%s' must reference an existing version key in [versions]", ref)
                    .containsPattern("(?m)^" + Pattern.quote(ref) + "\\s*=");
        }
    }

    @Test
    void versionKeys_shouldFollowNamingConvention() {
        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);

        List<String> versionKeys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();

        for (String key : versionKeys) {
            assertThat(key)
                    .as("Version key '%s' should use camelCase or lowercase naming convention", key)
                    .matches("[a-zA-Z][a-zA-Z0-9]*");
        }
    }
}
