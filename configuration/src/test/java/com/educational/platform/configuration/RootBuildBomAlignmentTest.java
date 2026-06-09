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
 * Validates that the root build.gradle.kts BOM import and the Spring Boot plugin
 * in libs.versions.toml both resolve to the same version. The root build imports
 * the BOM via {@code libs.versions.spring.get()}, which must reference the same
 * version key ("spring") used by the springboot plugin's version.ref. A mismatch
 * would cause the plugin to apply task configurations for one Spring Boot version
 * while the BOM manages dependencies for a different version, leading to
 * NoSuchMethodError or ClassNotFoundException at runtime.
 */
class RootBuildBomAlignmentTest {

    private static Path projectRoot;
    private static String rootBuildContent;
    private static String tomlContent;

    @BeforeAll
    static void loadBuildFiles() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        rootBuildContent = Files.readString(projectRoot.resolve("build.gradle.kts"));
        tomlContent = Files.readString(projectRoot.resolve("gradle/libs.versions.toml"));
    }

    @Test
    void rootBuild_shouldImportSpringBootBom() {
        assertThat(rootBuildContent)
                .as("Root build.gradle.kts must import the Spring Boot BOM for dependency management")
                .contains("spring-boot-dependencies");
    }

    @Test
    void rootBuild_bomImport_shouldUseVersionCatalogReference() {
        assertThat(rootBuildContent)
                .as("Root BOM import must use libs.versions.spring.get() from the version catalog, "
                        + "not a hardcoded version string")
                .containsPattern("spring-boot-dependencies.*libs\\.versions\\.spring\\.get\\(\\)");
    }

    @Test
    void rootBuild_bomVersion_shouldAlignWithPluginVersionRef() {
        // Extract the version key used by the springboot plugin
        int pluginsIdx = tomlContent.indexOf("[plugins]");
        assertThat(pluginsIdx).isGreaterThanOrEqualTo(0);
        String pluginsSection = tomlContent.substring(pluginsIdx);

        Matcher pluginVersionMatcher = Pattern.compile("springboot.*version\\.ref\\s*=\\s*\"(\\w+)\"")
                .matcher(pluginsSection);
        assertThat(pluginVersionMatcher.find())
                .as("springboot plugin must declare a version.ref")
                .isTrue();
        String pluginVersionKey = pluginVersionMatcher.group(1);

        // The root build references the same key via libs.versions.<key>.get()
        assertThat(rootBuildContent)
                .as("Root BOM import must reference the same version catalog key ('%s') "
                        + "as the springboot plugin to prevent version skew", pluginVersionKey)
                .containsPattern("libs\\.versions\\." + pluginVersionKey + "\\.get\\(\\)");
    }

    @Test
    void rootBuild_dependencyManagement_shouldBeInSubprojectsBlock() {
        int subprojectsIdx = rootBuildContent.indexOf("subprojects");
        int depMgmtIdx = rootBuildContent.indexOf("dependencyManagement");
        assertThat(subprojectsIdx)
                .as("Root build must have a subprojects block")
                .isGreaterThanOrEqualTo(0);
        assertThat(depMgmtIdx)
                .as("Root build must have a dependencyManagement block")
                .isGreaterThanOrEqualTo(0);
        assertThat(depMgmtIdx)
                .as("dependencyManagement must be inside subprojects block so all modules inherit the BOM")
                .isGreaterThan(subprojectsIdx);
    }

    @Test
    void rootBuild_shouldApplySpringDependencyManagementPlugin() {
        assertThat(rootBuildContent)
                .as("Root build must apply the spring-dependency-management plugin for BOM imports to work")
                .contains("libs.plugins.springdependencies");
    }

    @Test
    void rootBuild_shouldNotApplySpringBootPlugin() {
        assertThat(rootBuildContent)
                .as("Root build must NOT apply the Spring Boot plugin — only the configuration module applies it. "
                        + "Applying it at root would interfere with library module JAR packaging.")
                .doesNotContain("libs.plugins.springboot");
    }

    @Test
    void bomArtifactId_shouldBeExactlySpringBootDependencies() {
        assertThat(rootBuildContent)
                .as("BOM import must use the exact artifact 'spring-boot-dependencies' (not spring-boot-starter-parent)")
                .contains("spring-boot-dependencies")
                .doesNotContain("spring-boot-starter-parent");
    }

    @Test
    void bomGroupId_shouldBeOrgSpringframeworkBoot() {
        assertThat(rootBuildContent)
                .as("BOM import must use org.springframework.boot group coordinate")
                .containsPattern("org\\.springframework\\.boot.*spring-boot-dependencies");
    }
}
