package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Gradle wrapper is configured with a version that supports Java 26.
 * <p>
 * Java 26 support requires Gradle 9.4.0+. This test ensures the wrapper properties
 * are not accidentally downgraded to an incompatible version.
 */
public class GradleWrapperConfigTest {

    private static final String MINIMUM_GRADLE_VERSION = "9.4.0";
    private static final String EXPECTED_GRADLE_VERSION = "9.5.1";

    @Test
    void gradleWrapper_shouldUse_java26CompatibleVersion() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        String distributionUrl = props.getProperty("distributionUrl");
        assertThat(distributionUrl)
                .as("Gradle wrapper distributionUrl should be configured")
                .isNotNull()
                .contains("gradle-" + EXPECTED_GRADLE_VERSION);
    }

    @Test
    void gradleWrapper_distributionUrl_shouldPointToOfficialDistribution() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        String distributionUrl = props.getProperty("distributionUrl");
        assertThat(distributionUrl)
                .as("Distribution URL should use official Gradle services")
                .contains("services.gradle.org/distributions/");
    }

    @Test
    void gradleWrapper_shouldHave_distributionValidationEnabled() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        String validateDistributionUrl = props.getProperty("validateDistributionUrl");
        assertThat(validateDistributionUrl)
                .as("Distribution URL validation should be enabled for security")
                .isEqualTo("true");
    }

    private Path findGradleWrapperProperties() {
        // Walk up from working directory to find the project root
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("gradle/wrapper/gradle-wrapper.properties");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        // Fallback: try relative to standard project structure
        Path fallback = Paths.get("gradle/wrapper/gradle-wrapper.properties");
        assertThat(fallback)
                .as("gradle-wrapper.properties should exist in the project")
                .matches(Files::exists);
        return fallback;
    }
}
