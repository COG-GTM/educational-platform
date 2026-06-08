package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that all JUnit Platform and Jupiter artifacts on the classpath
 * resolve to the same version, ensuring BOM alignment after the Java 26 upgrade.
 * <p>
 * The PR added {@code junit-jupiter-params} (testImplementation) and
 * {@code junit-jupiter-engine} (testRuntimeOnly) alongside the pre-existing
 * {@code junit-jupiter-api}. If these resolve to different versions
 * (e.g., due to version conflict resolution), tests may fail unpredictably
 * with NoSuchMethodError or IncompatibleClassChangeError.
 * <p>
 * {@link TestDependencyScopeValidationTest} validates scopes.
 * {@link NewTestDependencyClasspathTest} validates classpath presence.
 * This test validates <em>version alignment</em> across the JUnit family.
 */
public class JUnitPlatformBomAlignmentTest {

    @Test
    void jupiterApi_and_jupiterParams_shouldHave_sameVersion() {
        String apiVersion = resolveArtifactVersion("org.junit.jupiter", "junit-jupiter-api");
        String paramsVersion = resolveArtifactVersion("org.junit.jupiter", "junit-jupiter-params");

        assertThat(apiVersion)
                .as("junit-jupiter-api version should be resolvable")
                .isNotNull();
        assertThat(paramsVersion)
                .as("junit-jupiter-params version should be resolvable")
                .isNotNull();

        assertThat(apiVersion)
                .as("junit-jupiter-api and junit-jupiter-params should have same version")
                .isEqualTo(paramsVersion);
    }

    @Test
    void jupiterApi_and_jupiterEngine_shouldHave_sameVersion() {
        String apiVersion = resolveArtifactVersion("org.junit.jupiter", "junit-jupiter-api");
        String engineVersion = resolveArtifactVersion("org.junit.jupiter", "junit-jupiter-engine");

        assertThat(apiVersion)
                .as("junit-jupiter-api version should be resolvable")
                .isNotNull();
        assertThat(engineVersion)
                .as("junit-jupiter-engine version should be resolvable")
                .isNotNull();

        assertThat(apiVersion)
                .as("junit-jupiter-api and junit-jupiter-engine should have same version")
                .isEqualTo(engineVersion);
    }

    @Test
    void platformEngine_and_platformLauncher_shouldHave_sameVersion() {
        String engineVersion = resolveArtifactVersion("org.junit.platform", "junit-platform-engine");
        String launcherVersion = resolveArtifactVersion("org.junit.platform", "junit-platform-launcher");

        assertThat(engineVersion)
                .as("junit-platform-engine version should be resolvable")
                .isNotNull();
        assertThat(launcherVersion)
                .as("junit-platform-launcher version should be resolvable")
                .isNotNull();

        assertThat(engineVersion)
                .as("junit-platform-engine and junit-platform-launcher should have same version")
                .isEqualTo(launcherVersion);
    }

    @ParameterizedTest(name = "JUnit artifact should be loadable: {0}")
    @ValueSource(strings = {
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.jupiter.engine.JupiterTestEngine",
            "org.junit.platform.engine.TestEngine",
            "org.junit.platform.launcher.Launcher"
    })
    void junitArtifact_shouldBeLoadable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("JUnit class '%s' should be loadable on Java 26", className)
                .doesNotThrowAnyException();
    }

    @Test
    void jupiterVersion_shouldBeAtLeast_5_10() {
        String apiVersion = resolveArtifactVersion("org.junit.jupiter", "junit-jupiter-api");
        assertThat(apiVersion).isNotNull();

        String[] parts = apiVersion.split("\\.");
        assertThat(parts.length).isGreaterThanOrEqualTo(2);

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);

        // JUnit 6.x (from Spring Boot 4.x BOM) or JUnit 5.10+ both support Java 26
        boolean meetsMinimum = (major > 5) || (major == 5 && minor >= 10);
        assertThat(meetsMinimum)
                .as("JUnit Jupiter version %s should be >= 5.10 for Java 26 support", apiVersion)
                .isTrue();
    }

    @Test
    void jupiterEngine_shouldBeDiscoverable_viaServiceLoader() {
        assertThatCode(() -> {
            var engineClass = Class.forName("org.junit.platform.engine.TestEngine");
            var loader = java.util.ServiceLoader.load(engineClass);
            long engineCount = loader.stream().count();

            assertThat(engineCount)
                    .as("JUnit Platform should discover at least one TestEngine via ServiceLoader")
                    .isGreaterThan(0);
        }).doesNotThrowAnyException();
    }

    private String resolveArtifactVersion(String groupId, String artifactId) {
        // Try pom.properties first
        String pomPropsPath = String.format("META-INF/maven/%s/%s/pom.properties", groupId, artifactId);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return props.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        // Fallback: resolve via Package.getImplementationVersion() of a known class
        try {
            String knownClass = switch (artifactId) {
                case "junit-jupiter-api" -> "org.junit.jupiter.api.Test";
                case "junit-jupiter-params" -> "org.junit.jupiter.params.ParameterizedTest";
                case "junit-jupiter-engine" -> "org.junit.jupiter.engine.JupiterTestEngine";
                case "junit-platform-engine" -> "org.junit.platform.engine.TestEngine";
                case "junit-platform-launcher" -> "org.junit.platform.launcher.Launcher";
                default -> null;
            };
            if (knownClass != null) {
                Class<?> clazz = Class.forName(knownClass);
                Package pkg = clazz.getPackage();
                if (pkg != null && pkg.getImplementationVersion() != null) {
                    return pkg.getImplementationVersion();
                }
            }
        } catch (ClassNotFoundException ignored) {
        }

        return null;
    }
}
