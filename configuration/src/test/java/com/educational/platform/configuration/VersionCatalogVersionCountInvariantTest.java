package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the exact set of version entries in the {@code [versions]} section
 * of gradle/libs.versions.toml. {@link VersionCatalogPluginCountInvariantTest}
 * guards the plugin count; this test guards against version sprawl — adding
 * unnecessary version entries creates ambiguity about which key controls
 * the Spring Boot ecosystem and risks shadowing BOM-managed versions.
 * <p>
 * The {@code spring} version key is shared by both the {@code springdependencies}
 * BOM import (via {@code libs.versions.spring.get()} in the root build) and the
 * {@code springboot} plugin (via {@code version.ref = "spring"}). An extra key
 * like {@code springBoot} or {@code spring-boot-plugin} would risk version skew.
 */
class VersionCatalogVersionCountInvariantTest {

    private static List<String> versionKeys;

    @BeforeAll
    static void loadVersionCatalog() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        String tomlContent = Files.readString(dir.resolve("gradle/libs.versions.toml"));

        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(versionsIdx).isGreaterThanOrEqualTo(0);
        assertThat(pluginsIdx).isGreaterThan(versionsIdx);

        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);
        versionKeys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
    }

    @Test
    void versionEntries_shouldHaveExpectedCount() {
        assertThat(versionKeys)
                .as("The [versions] section must contain exactly 10 entries — "
                        + "adding version entries without justification risks "
                        + "shadowing BOM-managed versions or creating confusion "
                        + "about which key controls a given dependency")
                .hasSize(10);
    }

    @Test
    void versionEntries_shouldContainSpringKey() {
        assertThat(versionKeys)
                .as("'spring' version key must be present — it is shared by the "
                        + "springboot plugin (version.ref) and the BOM import "
                        + "(libs.versions.spring.get())")
                .contains("spring");
    }

    @Test
    void versionEntries_shouldContainAllExpectedKeys() {
        Set<String> expectedKeys = Set.of(
                "spring",
                "restAssured",
                "mockito",
                "assertj",
                "archunit",
                "jsonwebtoken",
                "jaxbApi",
                "passay",
                "springDependencyManagementPlugin",
                "springDoc"
        );
        assertThat(versionKeys)
                .as("All expected version keys must be present — each is referenced "
                        + "by a plugin entry or a build script libs.versions accessor")
                .containsExactlyInAnyOrderElementsOf(expectedKeys);
    }

    @Test
    void versionEntries_shouldNotContainDuplicateSpringBootKey() {
        long springRelatedKeys = versionKeys.stream()
                .filter(key -> key.toLowerCase().contains("springboot")
                        || key.toLowerCase().contains("spring-boot"))
                .count();
        assertThat(springRelatedKeys)
                .as("There must be no 'springBoot' or 'spring-boot' version key — "
                        + "the 'spring' key already controls the Spring Boot plugin version "
                        + "and the BOM version. A separate key would risk version skew.")
                .isZero();
    }

    @Test
    void versionEntries_shouldNotHaveDuplicates() {
        assertThat(versionKeys)
                .as("Version keys must be unique — duplicates cause Gradle resolution ambiguity")
                .doesNotHaveDuplicates();
    }
}
