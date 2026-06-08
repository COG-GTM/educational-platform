package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the springboot plugin's version.ref in gradle/libs.versions.toml
 * explicitly references the "spring" version key. This is the critical BOM alignment
 * invariant: both the Spring Boot plugin (org.springframework.boot) and the
 * dependency-management plugin (io.spring.dependency-management) must resolve their
 * version from the same key so that plugin version and managed dependency versions
 * stay in lockstep. If the springboot plugin's version.ref were changed to a different
 * key (e.g., "springBoot"), the BOM and plugin could drift apart, causing
 * classpath conflicts or NoSuchMethodError at runtime.
 */
class VersionCatalogPluginVersionRefAlignmentTest {

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
        tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void springbootPlugin_versionRef_shouldBeExactlySpring() {
        String ref = extractVersionRef("springboot");
        assertThat(ref)
                .as("springboot plugin must use version.ref = \"spring\" — this key is shared "
                        + "with the BOM to guarantee plugin/dependency version alignment. "
                        + "Using a different key would allow silent version drift.")
                .isEqualTo("spring");
    }

    @Test
    void springbootPlugin_andBom_shouldResolveFromSameVersionValue() {
        String springbootRef = extractVersionRef("springboot");
        String springVersion = extractVersionValue("spring");

        assertThat(springbootRef).isNotNull();
        assertThat(springVersion).isNotNull();

        String springbootResolvedVersion = extractVersionValue(springbootRef);
        assertThat(springbootResolvedVersion)
                .as("The springboot plugin must resolve to the same version value as the 'spring' "
                        + "key used by the BOM — current spring = \"%s\"", springVersion)
                .isEqualTo(springVersion);
    }

    @Test
    void springVersionValue_shouldBe4_0_1() {
        String springVersion = extractVersionValue("spring");
        assertThat(springVersion)
                .as("The 'spring' version key must be 4.0.1 as declared in the PR")
                .isEqualTo("4.0.1");
    }

    @Test
    void springdependenciesPlugin_shouldNotReferenceSpringVersionKey() {
        String ref = extractVersionRef("springdependencies");
        assertThat(ref)
                .as("springdependencies plugin uses its own version key "
                        + "(springDependencyManagementPlugin), not the 'spring' key — "
                        + "this is correct because it is a separate plugin with its own release cycle")
                .isNotEqualTo("spring");
    }

    @Test
    void springbootPlugin_versionRef_shouldNotBeNull() {
        String ref = extractVersionRef("springboot");
        assertThat(ref)
                .as("springboot plugin must declare version.ref (not an inline version) "
                        + "to enable single-source version management via [versions]")
                .isNotNull();
    }

    private String extractVersionRef(String pluginName) {
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

    private String extractVersionValue(String versionKey) {
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
}
