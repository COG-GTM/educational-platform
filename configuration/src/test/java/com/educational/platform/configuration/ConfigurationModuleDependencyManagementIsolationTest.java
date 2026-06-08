package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the invariant that the configuration module must NOT declare its own
 * {@code dependencyManagement} block or redundant plugin applications. The root
 * build.gradle.kts already applies {@code io.spring.dependency-management} and
 * imports the Spring Boot BOM for ALL subprojects. Redeclaring these in the
 * configuration module would:
 * <ul>
 *   <li>Create version precedence ambiguity between root and module-level BOMs</li>
 *   <li>Override the centralized version strategy if a different BOM version is used</li>
 *   <li>Cause subtle classpath issues when plugin-managed versions diverge from BOM versions</li>
 * </ul>
 * The Spring Boot plugin (applied in this module) already coordinates with the
 * dependency-management plugin from the root; no additional configuration is needed.
 */
class ConfigurationModuleDependencyManagementIsolationTest {

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
    void buildFile_shouldNotDeclareDependencyManagementBlock() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT have a dependencyManagement block — "
                        + "the root build already imports the Spring Boot BOM for all subprojects "
                        + "via io.spring.dependency-management; redeclaring it would risk version skew")
                .doesNotContainPattern("dependencyManagement\\s*\\{");
    }

    @Test
    void buildFile_shouldNotRedeclareSpringDependencyManagementPlugin() {
        assertThat(buildContent)
                .as("configuration module must NOT apply io.spring.dependency-management — "
                        + "it is already applied to all subprojects by the root build")
                .doesNotContain("io.spring.dependency-management")
                .doesNotContain("springdependencies");
    }

    @Test
    void buildFile_shouldNotRedeclareJavaPlugin() {
        assertThat(buildContent)
                .as("configuration module must NOT apply the 'java' plugin — "
                        + "it is applied to all subprojects via the root build's apply block")
                .doesNotContainPattern("\\bplugin.*\"java\"")
                .doesNotContainPattern("\\bjava\\b.*\\{");
    }

    @Test
    void buildFile_shouldNotRedeclareJavaLibraryPlugin() {
        assertThat(buildContent)
                .as("configuration module must NOT apply 'java-library' — "
                        + "it is applied to all subprojects by the root build; the configuration module "
                        + "is the boot entry point and should only expose bootJar, not a library API")
                .doesNotContain("java-library");
    }

    @Test
    void buildFile_shouldNotImportBomDirectly() {
        assertThat(buildContent)
                .as("configuration module must NOT import a BOM directly via platform() or enforcedPlatform() — "
                        + "BOM management is centralized in the root build")
                .doesNotContain("platform(")
                .doesNotContain("enforcedPlatform(");
    }

    @Test
    void buildFile_shouldNotDeclareConfigurationsBlock() {
        assertThat(buildContent)
                .as("configuration module must NOT have a configurations {} block — "
                        + "custom configuration modifications could interfere with the Spring Boot "
                        + "plugin's dependency resolution and BOM management")
                .doesNotContainPattern("^configurations\\s*\\{");
    }
}
