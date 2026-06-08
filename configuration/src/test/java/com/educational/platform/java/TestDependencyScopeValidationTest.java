package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that test dependencies in configuration/build.gradle.kts use the
 * correct Gradle scope (testImplementation vs testRuntimeOnly).
 * <p>
 * The Java 26 upgrade added three new test dependencies:
 * <ul>
 *   <li>{@code junit-jupiter-params} — testImplementation (compile-time annotations)</li>
 *   <li>{@code junit-jupiter-engine} — testRuntimeOnly (discovered via ServiceLoader at runtime)</li>
 *   <li>{@code assertj-core} — testImplementation (compile-time fluent assertions)</li>
 * </ul>
 * Using the wrong scope causes either compile errors (if runtime-only) or
 * unnecessary coupling (if implementation-only deps are exposed at compile time).
 * <p>
 * {@link NewTestDependencyClasspathTest} verifies these dependencies are on
 * the classpath. This test verifies they are declared with the <em>correct scope</em>.
 */
public class TestDependencyScopeValidationTest {

    @Test
    void jupiterEngine_shouldBe_testRuntimeOnly() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("junit-jupiter-engine should be declared as testRuntimeOnly")
                .containsPattern("testRuntimeOnly\\s*\\(.*junit-jupiter-engine.*\\)");

        assertThat(content)
                .as("junit-jupiter-engine should NOT be testImplementation")
                .doesNotContainPattern("testImplementation\\s*\\(.*junit-jupiter-engine.*\\)");
    }

    @Test
    void jupiterParams_shouldBe_testImplementation() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("junit-jupiter-params should be declared as testImplementation")
                .containsPattern("testImplementation\\s*\\(.*junit-jupiter-params.*\\)");
    }

    @Test
    void assertjCore_shouldBe_testImplementation() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("assertj-core should be declared as testImplementation")
                .containsPattern("testImplementation\\s*\\(.*assertj-core.*\\)");
    }

    @Test
    void jupiterApi_shouldBe_testImplementation() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("junit-jupiter-api should be declared as testImplementation")
                .containsPattern("testImplementation\\s*\\(.*junit-jupiter-api.*\\)");
    }

    @Test
    void mockitoJupiter_shouldBe_testImplementation() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("mockito-junit-jupiter should be declared as testImplementation")
                .containsPattern("testImplementation\\s*\\(.*mockito-junit-jupiter.*\\)");
    }

    @Test
    void archunitJunit5_shouldBe_testImplementation() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("archunit-junit5 should be declared as testImplementation")
                .containsPattern("testImplementation\\s*\\(.*archunit-junit5.*\\)");
    }

    @ParameterizedTest(name = "Dependency ''{0}'' should use version from catalog via ''{1}''")
    @CsvSource({
            "mockito-junit-jupiter, libs.versions.mockito.get()",
            "assertj-core,          libs.versions.assertj.get()",
            "archunit-junit5,       libs.versions.archunit.get()"
    })
    void versionCatalogManaged_dependency_shouldReference_catalogVersion(
            String dependency, String catalogRef) throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("'%s' declaration should reference version catalog via %s", dependency, catalogRef)
                .contains(dependency)
                .contains(catalogRef);
    }

    @Test
    void onlyJupiterEngine_shouldBe_testRuntimeOnly() throws IOException {
        String content = readConfigBuildGradle();

        List<String> lines = content.lines().toList();
        long runtimeOnlyCount = lines.stream()
                .filter(line -> line.trim().startsWith("testRuntimeOnly"))
                .count();

        assertThat(runtimeOnlyCount)
                .as("Only junit-jupiter-engine should use testRuntimeOnly scope")
                .isEqualTo(1);
    }

    @Test
    void versionCatalogKeys_usedInBuild_shouldExistInCatalog() throws IOException {
        String buildContent = readConfigBuildGradle();
        String catalogContent = readVersionCatalog();

        Pattern catalogRefPattern = Pattern.compile("libs\\.versions\\.(\\w+)\\.get\\(\\)");
        Matcher matcher = catalogRefPattern.matcher(buildContent);

        while (matcher.find()) {
            String key = matcher.group(1);
            assertThat(catalogContent)
                    .as("Version catalog key '%s' referenced in build.gradle.kts should exist in libs.versions.toml", key)
                    .containsPattern(key + "\\s*=\\s*\"[^\"]+\"");
        }
    }

    private String readConfigBuildGradle() throws IOException {
        return Files.readString(findProjectRoot().resolve("configuration/build.gradle.kts"));
    }

    private String readVersionCatalog() throws IOException {
        return Files.readString(findProjectRoot().resolve("gradle/libs.versions.toml"));
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
