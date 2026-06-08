package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the three new test dependencies added in
 * {@code configuration/build.gradle.kts} as part of the Java 26 upgrade
 * are properly declared and available on the test classpath.
 * <p>
 * The PR added:
 * <ul>
 *   <li>{@code junit-jupiter-params} — parameterized test support</li>
 *   <li>{@code junit-jupiter-engine} — JUnit 5 test engine</li>
 *   <li>{@code assertj-core} — fluent assertion library</li>
 * </ul>
 * {@link DependencyResolutionVerificationTest} and {@link DependencyVersionTest}
 * check general dependency resolution. This test validates the <em>specific</em>
 * new dependencies added by this PR are on the classpath with functional APIs.
 */
public class NewTestDependencyClasspathTest {

    // --- junit-jupiter-params availability ---

    @Test
    void junitJupiterParams_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.params.ParameterizedTest"))
                .as("@ParameterizedTest should be loadable (junit-jupiter-params)")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Params annotation class should be available: {0}")
    @ValueSource(strings = {
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.jupiter.params.provider.MethodSource",
            "org.junit.jupiter.params.provider.CsvSource",
            "org.junit.jupiter.params.provider.ValueSource",
            "org.junit.jupiter.params.provider.Arguments",
            "org.junit.jupiter.params.provider.EnumSource"
    })
    void jupiterParamsAnnotation_shouldBeLoadable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("'%s' should be loadable from junit-jupiter-params", className)
                .doesNotThrowAnyException();
    }

    // --- junit-jupiter-engine availability ---

    @Test
    void junitJupiterEngine_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.junit.jupiter.engine.JupiterTestEngine"))
                .as("JupiterTestEngine should be loadable (junit-jupiter-engine)")
                .doesNotThrowAnyException();
    }

    @Test
    void junitPlatformLauncher_shouldDiscoverJupiterEngine() {
        assertThatCode(() -> {
            var loader = java.util.ServiceLoader.load(
                    Class.forName("org.junit.platform.engine.TestEngine").asSubclass(Object.class));
            assertThat(loader.stream().count())
                    .as("At least one TestEngine should be discoverable via ServiceLoader")
                    .isGreaterThan(0);
        }).doesNotThrowAnyException();
    }

    // --- assertj-core availability ---

    @Test
    void assertjCore_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.assertj.core.api.Assertions"))
                .as("Assertions should be loadable (assertj-core)")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "AssertJ class should be available: {0}")
    @ValueSource(strings = {
            "org.assertj.core.api.Assertions",
            "org.assertj.core.api.SoftAssertions",
            "org.assertj.core.api.AbstractAssert",
            "org.assertj.core.api.ListAssert",
            "org.assertj.core.api.MapAssert"
    })
    void assertjClass_shouldBeLoadable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("'%s' should be loadable from assertj-core", className)
                .doesNotThrowAnyException();
    }

    // --- build.gradle.kts declares the new dependencies ---

    @Test
    void buildGradleKts_shouldDeclare_junitJupiterParams() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration/build.gradle.kts should declare junit-jupiter-params")
                .contains("junit-jupiter-params");
    }

    @Test
    void buildGradleKts_shouldDeclare_junitJupiterEngine() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration/build.gradle.kts should declare junit-jupiter-engine")
                .contains("junit-jupiter-engine");
    }

    @Test
    void buildGradleKts_shouldDeclare_assertjCore() throws IOException {
        String content = readConfigBuildGradle();

        assertThat(content)
                .as("configuration/build.gradle.kts should declare assertj-core")
                .contains("assertj-core");
    }

    // --- version catalog references should exist ---

    @Test
    void versionCatalog_shouldDefine_assertjVersion() throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);

        assertThat(content)
                .as("libs.versions.toml should define assertj version")
                .containsPattern("assertj\\s*=\\s*\"[0-9]+\\.[0-9]+\\.[0-9]+\"");
    }

    @Test
    void versionCatalog_shouldDefine_mockitoVersion() throws IOException {
        Path catalog = findProjectRoot().resolve("gradle/libs.versions.toml");
        String content = Files.readString(catalog);

        assertThat(content)
                .as("libs.versions.toml should define mockito version")
                .containsPattern("mockito\\s*=\\s*\"[0-9]+\\.[0-9]+\\.[0-9]+\"");
    }

    // --- functional integration: new dependencies work together ---

    @ParameterizedTest(name = "CsvSource integration: {0} + {1} = {2}")
    @CsvSource({
            "junit-jupiter-params, assertj-core, test-stack",
            "junit-jupiter-engine, junit-jupiter-params, discovery"
    })
    void newDependencies_shouldWorkTogether_inParameterizedTest(String dep1, String dep2, String purpose) {
        assertThat(dep1).isNotEmpty();
        assertThat(dep2).isNotEmpty();
        assertThat(purpose).isNotBlank();
    }

    private String readConfigBuildGradle() throws IOException {
        Path root = findProjectRoot();
        return Files.readString(root.resolve("configuration/build.gradle.kts"));
    }

    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) return current;
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir"));
    }
}
