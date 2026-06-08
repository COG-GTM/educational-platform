package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against accidentally defining Spring Boot dependencies in a
 * [libraries] section of gradle/libs.versions.toml. The project uses
 * the Spring Boot BOM (via io.spring.dependency-management) for transitive
 * version management — all Spring Boot starters are declared inline in
 * build.gradle.kts with two-argument (group, artifact) notation and no
 * explicit version. Defining them in [libraries] would create a parallel
 * version management path that could diverge from the BOM.
 * <p>
 * The TOML currently has only [versions] and [plugins] sections.
 * Complements {@link StarterTestDependencyNotationValidationTest} (inline
 * notation format) and {@link PluginBomRuntimeVersionCrossValidationTest}
 * (version pipeline integrity).
 */
class VersionCatalogLibrariesSectionGuardTest {

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
    void toml_shouldNotHaveLibrariesSection() {
        assertThat(tomlContent)
                .as("libs.versions.toml must NOT contain a [libraries] section — "
                        + "Spring Boot dependencies are version-managed by the BOM imported "
                        + "in root build.gradle.kts, not by catalog library entries")
                .doesNotContain("[libraries]");
    }

    @Test
    void toml_shouldNotDefineSpringBootStarterTest() {
        assertThat(tomlContent)
                .as("spring-boot-starter-test must NOT appear in libs.versions.toml — "
                        + "it is declared inline in configuration/build.gradle.kts with "
                        + "version managed by the Spring Boot BOM")
                .doesNotContain("spring-boot-starter-test");
    }

    @Test
    void toml_shouldNotDefineSpringBootStarterWeb() {
        assertThat(tomlContent)
                .as("spring-boot-starter-web must NOT appear in libs.versions.toml — "
                        + "it is declared inline in configuration/build.gradle.kts with "
                        + "version managed by the Spring Boot BOM")
                .doesNotContain("spring-boot-starter-web");
    }

    @Test
    void toml_shouldNotDefineLiquibaseCore() {
        assertThat(tomlContent)
                .as("liquibase-core must NOT appear in libs.versions.toml — "
                        + "it is declared inline in configuration/build.gradle.kts with "
                        + "version managed by the Spring Boot BOM")
                .doesNotContain("liquibase-core");
    }

    @Test
    void toml_shouldHaveExactlyTwoSections() {
        long sectionCount = tomlContent.lines()
                .filter(line -> line.trim().startsWith("[") && !line.trim().startsWith("[["))
                .count();
        assertThat(sectionCount)
                .as("libs.versions.toml must have exactly 2 sections: [versions] and [plugins] — "
                        + "adding [libraries] or [bundles] would introduce version management "
                        + "that conflicts with BOM-based management")
                .isEqualTo(2);
    }

    @Test
    void toml_shouldNotHaveBundlesSection() {
        assertThat(tomlContent)
                .as("libs.versions.toml must NOT contain a [bundles] section — "
                        + "dependency grouping is handled by the configuration module's "
                        + "build.gradle.kts, not by catalog bundles")
                .doesNotContain("[bundles]");
    }
}
