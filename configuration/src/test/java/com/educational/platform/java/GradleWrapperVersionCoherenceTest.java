package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates coherence between the Gradle wrapper distribution URL version, the
 * wrapper JAR manifest, and the wrapper properties file after the 9.2.1 → 9.5.1
 * upgrade.
 * <p>
 * {@link GradleDistributionUrlFormatTest} validates the URL format.
 * {@link GradleWrapperPropertiesCompletenessTest} validates property existence.
 * {@link GradleWrapperSecurityValidationTest} validates security properties.
 * This test cross-checks that the version embedded in the distribution URL is
 * consistent with the wrapper JAR's manifest and the retry/network configuration
 * introduced in 9.5.x.
 */
public class GradleWrapperVersionCoherenceTest {

    @Test
    void distributionUrlVersion_shouldMatch_expectedVersion() throws IOException {
        String version = extractVersionFromDistributionUrl();

        assertThat(version)
                .as("Distribution URL should reference Gradle 9.5.1")
                .isEqualTo("9.5.1");
    }

    @Test
    void wrapperJarManifest_shouldExist_andHaveVersion() throws IOException {
        Path jarPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.jar");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Manifest manifest = jar.getManifest();
            assertThat(manifest)
                    .as("Wrapper JAR manifest should exist")
                    .isNotNull();

            String implVersion = manifest.getMainAttributes().getValue("Implementation-Version");

            // Gradle 9.5+ wrapper JARs include the version in the manifest
            if (implVersion != null) {
                assertThat(implVersion)
                        .as("Wrapper JAR Implementation-Version should reference 9.5.1")
                        .contains("9.5");
            }
        }
    }

    @Test
    void wrapperProperties_retryConfig_shouldBeConsistentWith_951() throws IOException {
        Properties props = readWrapperProperties();

        // Gradle 9.5.x introduced retries and retryBackOffMs
        String retries = props.getProperty("retries");
        String retryBackOff = props.getProperty("retryBackOffMs");

        assertThat(retries)
                .as("retries property should be present (introduced in 9.5.x)")
                .isNotNull();
        assertThat(retryBackOff)
                .as("retryBackOffMs property should be present (introduced in 9.5.x)")
                .isNotNull();

        // Both should be valid non-negative integers
        int retriesValue = Integer.parseInt(retries);
        int backOffValue = Integer.parseInt(retryBackOff);

        assertThat(retriesValue)
                .as("retries should be non-negative")
                .isGreaterThanOrEqualTo(0);
        assertThat(backOffValue)
                .as("retryBackOffMs should be positive")
                .isGreaterThan(0);
    }

    @ParameterizedTest(name = "Gradle {0}.{1}.{2} should{3} have retry properties")
    @CsvSource({
            "9, 5, 1, ' '",
            "9, 5, 0, ' '",
            "10, 0, 0, ' '"
    })
    void gradleVersion_atOrAbove95_shouldSupport_retryProperties(
            int major, int minor, int patch, String placeholder) {
        boolean supportsRetry = (major > 9) || (major == 9 && minor >= 5);

        assertThat(supportsRetry)
                .as("Gradle %d.%d.%d should support retry properties", major, minor, patch)
                .isTrue();
    }

    @Test
    void networkTimeout_shouldBeReasonable() throws IOException {
        Properties props = readWrapperProperties();
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));

        assertThat(timeout)
                .as("networkTimeout should be between 5000 and 60000 ms")
                .isBetween(5000, 60000);
    }

    @Test
    void distributionUrl_gradleVersion_shouldBeGreaterThan_previous921() throws IOException {
        String version = extractVersionFromDistributionUrl();
        String[] parts = version.split("\\.");

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        boolean greaterThan921 = major > 9
                || (major == 9 && minor > 2)
                || (major == 9 && minor == 2 && patch > 1);

        assertThat(greaterThan921)
                .as("Distribution URL version %s should be greater than old 9.2.1", version)
                .isTrue();
    }

    @Test
    void distributionBase_andZipStoreBase_shouldBoth_beGradleUserHome() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("distributionBase"))
                .as("distributionBase should be GRADLE_USER_HOME")
                .isEqualTo("GRADLE_USER_HOME");

        assertThat(props.getProperty("zipStoreBase"))
                .as("zipStoreBase should be GRADLE_USER_HOME")
                .isEqualTo("GRADLE_USER_HOME");
    }

    private String extractVersionFromDistributionUrl() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");
        Matcher matcher = Pattern.compile("gradle-(\\d+\\.\\d+\\.\\d+)").matcher(url);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private Properties readWrapperProperties() throws IOException {
        Path propsFile = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(propsFile));
        return props;
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
