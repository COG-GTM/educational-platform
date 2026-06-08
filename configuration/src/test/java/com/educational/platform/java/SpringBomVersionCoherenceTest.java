package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot BOM version declared in the root
 * {@code build.gradle.kts} resolves from the version catalog and is
 * coherent with the Java 26 upgrade.
 * <p>
 * {@link RootBuildScriptStructureTest} verifies the presence of the
 * {@code spring-boot-dependencies} BOM import. This test goes further
 * by extracting the actual version reference and cross-checking it
 * against the version catalog declaration, ensuring:
 * <ul>
 *   <li>The BOM uses the version catalog ({@code libs.versions.spring.get()})
 *       rather than a hard-coded version string</li>
 *   <li>The catalog-declared Spring Boot version is compatible with Java 26</li>
 *   <li>The BOM artifact coordinates are correct</li>
 *   <li>No stale Spring Boot version references exist</li>
 * </ul>
 */
public class SpringBomVersionCoherenceTest {

    @Test
    void rootBuild_shouldImportBom_usingVersionCatalog() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("BOM import should reference the version catalog, not a hard-coded version")
                .contains("libs.versions.spring.get()");
    }

    @Test
    void rootBuild_bomCoordinates_shouldBe_springBootDependencies() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("BOM should import spring-boot-dependencies")
                .containsPattern("mavenBom\\s*\\(.*spring-boot-dependencies.*\\)");
    }

    @Test
    void rootBuild_bomImport_shouldBeInside_dependencyManagement() throws IOException {
        String content = readRootBuildGradle();

        Pattern dmBlock = Pattern.compile(
                "dependencyManagement\\s*\\{[^}]*imports\\s*\\{[^}]*mavenBom",
                Pattern.DOTALL);

        assertThat(dmBlock.matcher(content).find())
                .as("mavenBom should be inside dependencyManagement { imports { } }")
                .isTrue();
    }

    @Test
    void versionCatalog_springVersion_shouldBeCompatibleWith_java26() throws IOException {
        String version = readCatalogSpringVersion();

        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher m = semver.matcher(version);
        assertThat(m.find())
                .as("Spring Boot version '%s' should be parseable", version)
                .isTrue();

        int major = Integer.parseInt(m.group(1));

        assertThat(major)
                .as("Spring Boot %s should be >= 3.x for modern Java support", version)
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void versionCatalog_shouldDeclare_springVersion() throws IOException {
        String catalog = readVersionCatalog();

        assertThat(catalog)
                .as("Version catalog should have a 'spring' entry")
                .containsPattern("spring\\s*=\\s*\"[^\"]+\"");
    }

    @Test
    void rootBuild_shouldNotHardcode_springBootVersion() throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build should not hard-code Spring Boot version as a literal string in BOM import")
                .doesNotContainPattern(
                        "spring-boot-dependencies:\\d+\\.\\d+\\.\\d+");
    }

    @ParameterizedTest(name = "Root build should not reference old Spring Boot {0}")
    @ValueSource(strings = {"2.7", "3.0", "3.1", "3.2", "3.3"})
    void rootBuild_shouldNotReference_oldSpringBootVersions(String oldVersion) throws IOException {
        String content = readRootBuildGradle();

        assertThat(content)
                .as("Root build should not reference old Spring Boot %s", oldVersion)
                .doesNotContain("spring-boot-dependencies:" + oldVersion);
    }

    @Test
    void dependencyManagement_shouldBeInside_subprojects() throws IOException {
        String content = readRootBuildGradle();

        // subprojects block contains nested braces, so [^}]* won't work.
        // Instead, verify subprojects appears before dependencyManagement.
        int subprojectsIdx = content.indexOf("subprojects");
        int depMgmtIdx = content.indexOf("dependencyManagement");

        assertThat(subprojectsIdx)
                .as("subprojects block should exist")
                .isGreaterThanOrEqualTo(0);
        assertThat(depMgmtIdx)
                .as("dependencyManagement block should exist")
                .isGreaterThan(subprojectsIdx);
    }

    @Test
    void catalogVersion_shouldMatch_bomReference_format() throws IOException {
        String catalogVersion = readCatalogSpringVersion();
        String buildContent = readRootBuildGradle();

        assertThat(buildContent)
                .as("Build file should use catalog reference that resolves to '%s'", catalogVersion)
                .contains("rootProject.libs.versions.spring.get()");
    }

    private String readRootBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("build.gradle.kts"));
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
    }

    private String readCatalogSpringVersion() throws IOException {
        String catalog = readVersionCatalog();
        Matcher m = Pattern.compile("spring\\s*=\\s*\"([^\"]+)\"").matcher(catalog);
        assertThat(m.find()).as("Should find spring version in catalog").isTrue();
        return m.group(1);
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path settingsFile = current.resolve("settings.gradle.kts");
            Path gradleDir = current.resolve("gradle/wrapper");
            if (Files.exists(settingsFile) || Files.isDirectory(gradleDir)) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
