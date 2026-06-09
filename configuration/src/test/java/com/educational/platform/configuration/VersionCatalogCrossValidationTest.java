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
 * Cross-validates the Spring Boot version declared in gradle/libs.versions.toml
 * against the actual runtime version reported by SpringBootVersion.getVersion().
 * A mismatch indicates the plugin version and the BOM-managed dependencies are
 * out of sync, which would cause NoSuchMethodError or ClassNotFoundException
 * at bootRun startup.
 */
class VersionCatalogCrossValidationTest {

    private static String tomlContent;
    private static String declaredVersion;

    @BeforeAll
    static void loadAndParseVersionCatalog() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));

        Matcher m = Pattern.compile("^spring\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE)
                .matcher(tomlContent);
        assertThat(m.find())
                .as("libs.versions.toml must declare a 'spring' version")
                .isTrue();
        declaredVersion = m.group(1);
    }

    @Test
    void runtimeVersion_shouldMatchDeclaredVersion() {
        assertThat(SpringBootVersion.getVersion())
                .as("Runtime Spring Boot version must exactly match the version declared in libs.versions.toml "
                        + "(spring = \"%s\")", declaredVersion)
                .isEqualTo(declaredVersion);
    }

    @Test
    void declaredVersion_shouldFollowSemanticVersioning() {
        assertThat(declaredVersion)
                .as("Declared spring version in TOML must follow semver (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+.*");
    }

    @Test
    void runtimeMajorVersion_shouldMatchDeclaredMajor() {
        String runtimeMajor = SpringBootVersion.getVersion().split("\\.")[0];
        String declaredMajor = declaredVersion.split("\\.")[0];
        assertThat(runtimeMajor)
                .as("Runtime major version must match declared major version to prevent API incompatibility")
                .isEqualTo(declaredMajor);
    }

    @Test
    void springbootPlugin_versionRef_shouldPointToSpringVersion() {
        // Extract the version.ref for the springboot plugin
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);

        Matcher m = Pattern.compile("springboot.*version\\.ref\\s*=\\s*\"(\\w+)\"")
                .matcher(pluginsSection);
        assertThat(m.find())
                .as("springboot plugin must use version.ref")
                .isTrue();
        String versionRefKey = m.group(1);

        // Verify the referenced version key resolves to the declared version
        Matcher versionMatcher = Pattern.compile(
                "^" + Pattern.quote(versionRefKey) + "\\s*=\\s*\"([^\"]+)\"",
                Pattern.MULTILINE
        ).matcher(tomlContent);
        assertThat(versionMatcher.find())
                .as("version.ref '%s' must resolve to a declared version", versionRefKey)
                .isTrue();
        assertThat(versionMatcher.group(1))
                .as("springboot plugin version.ref must resolve to the same value as runtime version")
                .isEqualTo(SpringBootVersion.getVersion());
    }
}
