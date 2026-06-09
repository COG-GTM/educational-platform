package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the configuration module's build.gradle.kts overriding
 * settings that the root build already provides to all subprojects.
 * The root build applies java, java-library, io.spring.dependency-management,
 * sets sourceCompatibility/targetCompatibility, declares repositories, and
 * imports the Spring Boot BOM. Duplicating any of these in the configuration
 * module would create maintenance burden and risk version skew with the
 * Spring Boot plugin added by this PR.
 */
class ConfigurationBuildFileInheritanceGuardTest {

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
    void buildFile_shouldNotDeclareOwnRepositories() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT declare repositories — "
                        + "the root build's allprojects block already declares mavenCentral() "
                        + "for all subprojects; a local override could cause dependency "
                        + "resolution inconsistency with the Spring Boot BOM")
                .doesNotContainPattern("repositories\\s*\\{");
    }

    @Test
    void buildFile_shouldNotDeclareSourceCompatibility() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT set sourceCompatibility — "
                        + "the root build's subprojects block sets JavaVersion.VERSION_25 "
                        + "for all modules; overriding here risks compiling against a "
                        + "different language level than the Spring Boot plugin expects")
                .doesNotContainPattern("sourceCompatibility\\s*=");
    }

    @Test
    void buildFile_shouldNotDeclareTargetCompatibility() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT set targetCompatibility — "
                        + "the root build already configures this for all subprojects")
                .doesNotContainPattern("targetCompatibility\\s*=");
    }

    @Test
    void buildFile_shouldNotApplyJavaPlugin() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT apply the java plugin — "
                        + "the root build's subprojects block already applies it; "
                        + "the Spring Boot plugin also implicitly applies java")
                .doesNotContainPattern("plugin.*[\"']java[\"']")
                .doesNotContain("apply(plugin = \"java\")")
                .doesNotContain("id(\"java\")");
    }

    @Test
    void buildFile_shouldNotApplyJavaLibraryPlugin() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT explicitly apply java-library — "
                        + "the root build's subprojects block already applies it")
                .doesNotContain("java-library");
    }

    @Test
    void buildFile_shouldNotDeclareDependencyManagement() {
        assertThat(buildContent)
                .as("configuration/build.gradle.kts must NOT declare its own dependencyManagement — "
                        + "the root build imports the Spring Boot BOM for all subprojects; "
                        + "a local override would create BOM version skew with the Spring Boot plugin")
                .doesNotContainPattern("dependencyManagement\\s*\\{");
    }

    @Test
    void buildFile_shouldNotDeclareGroup() {
        List<String> groupLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("group") && line.contains("="))
                .filter(line -> !line.startsWith("//"))
                .toList();
        assertThat(groupLines)
                .as("configuration/build.gradle.kts must NOT set group — "
                        + "the root build's allprojects block already sets com.educational.platform")
                .isEmpty();
    }

    @Test
    void buildFile_shouldNotDeclareVersion() {
        List<String> versionLines = buildContent.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("version") && line.contains("="))
                .filter(line -> !line.startsWith("//"))
                .toList();
        assertThat(versionLines)
                .as("configuration/build.gradle.kts must NOT set version — "
                        + "the root build's allprojects block already sets 0.0.1-SNAPSHOT")
                .isEmpty();
    }
}
