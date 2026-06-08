package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the exact set of version keys declared in the [versions] section
 * of gradle/libs.versions.toml. The PR added the springboot plugin entry that
 * reuses the existing "spring" version key. These tests guard against:
 * <ul>
 *   <li>Accidental addition of redundant version keys (e.g., "springBoot"
 *       alongside "spring") that would create ambiguity about which key
 *       controls the Spring Boot ecosystem</li>
 *   <li>Removal of required version keys referenced by build scripts via
 *       {@code libs.versions.<key>.get()}</li>
 *   <li>Version key naming inconsistencies (camelCase vs kebab-case)</li>
 * </ul>
 */
class VersionCatalogVersionKeyCompletenessTest {

    private static final Set<String> EXPECTED_VERSION_KEYS = Set.of(
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

    private static String tomlContent;
    private static List<String> actualVersionKeys;

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

        int versionsIdx = tomlContent.indexOf("[versions]");
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        String versionsSection = tomlContent.substring(
                versionsIdx + "[versions]".length(), pluginsIdx);

        actualVersionKeys = versionsSection.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split("\\s*=")[0].trim())
                .toList();
    }

    @Test
    void versionKeys_shouldContainExactlyExpectedKeys() {
        assertThat(actualVersionKeys)
                .as("The [versions] section must contain exactly the expected version keys — "
                        + "adding or removing keys requires updating both the TOML and build scripts")
                .containsExactlyInAnyOrderElementsOf(EXPECTED_VERSION_KEYS);
    }

    @Test
    void versionKeys_shouldHaveExpectedCount() {
        assertThat(actualVersionKeys)
                .as("The [versions] section must have exactly %d entries", EXPECTED_VERSION_KEYS.size())
                .hasSize(EXPECTED_VERSION_KEYS.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"springBoot", "spring-boot", "springboot", "boot"})
    void versionKeys_shouldNotContainRedundantSpringBootKey(String redundantKey) {
        assertThat(actualVersionKeys)
                .as("Version key '%s' must not exist — the springboot plugin reuses the "
                        + "'spring' version key to ensure BOM alignment", redundantKey)
                .doesNotContain(redundantKey);
    }

    @Test
    void springVersionKey_shouldBePresent() {
        assertThat(actualVersionKeys)
                .as("'spring' version key must be present — it controls both the "
                        + "dependency-management BOM and the Spring Boot plugin version")
                .contains("spring");
    }

    @Test
    void versionKeys_shouldUseCamelCaseNamingConvention() {
        for (String key : actualVersionKeys) {
            assertThat(key)
                    .as("Version key '%s' must use camelCase (no hyphens or underscores) "
                            + "to match Gradle's version catalog accessor convention", key)
                    .doesNotContain("-")
                    .doesNotContain("_");
        }
    }
}
