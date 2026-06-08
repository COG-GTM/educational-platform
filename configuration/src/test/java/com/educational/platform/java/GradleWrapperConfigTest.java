package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Test
    void gradleWrapper_shouldHave_retriesConfigured() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        String retries = props.getProperty("retries");
        assertThat(retries)
                .as("Gradle wrapper retries property should be set")
                .isNotNull();
    }

    @Test
    void gradleWrapper_shouldHave_retryBackOffMsConfigured() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        String retryBackOffMs = props.getProperty("retryBackOffMs");
        assertThat(retryBackOffMs)
                .as("Gradle wrapper retryBackOffMs property should be set")
                .isNotNull();
    }

    @Test
    void gradleWrapper_version_shouldBeAtLeast_minimumForJava26() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        String distributionUrl = props.getProperty("distributionUrl");
        assertThat(distributionUrl).isNotNull();

        Pattern versionPattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = versionPattern.matcher(distributionUrl);
        assertThat(matcher.find())
                .as("Distribution URL should contain a parseable Gradle version")
                .isTrue();

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));

        // Java 26 requires Gradle >= 9.4.0
        assertThat(major).as("Gradle major version").isGreaterThanOrEqualTo(9);
        if (major == 9) {
            assertThat(minor).as("Gradle minor version (when major=9)").isGreaterThanOrEqualTo(4);
        }
    }

    @Test
    void gradleWrapper_jarFile_shouldExist() {
        Path wrapperProperties = findGradleWrapperProperties();
        Path wrapperJar = wrapperProperties.getParent().resolve("gradle-wrapper.jar");

        assertThat(wrapperJar)
                .as("Gradle wrapper JAR should exist alongside wrapper properties")
                .exists();
    }

    @Test
    void gradleWrapper_shouldContainAllRequiredProperties() throws IOException {
        Path wrapperProperties = findGradleWrapperProperties();

        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProperties));

        assertThat(props.stringPropertyNames())
                .as("Wrapper properties should contain all required keys")
                .contains(
                        "distributionBase",
                        "distributionPath",
                        "distributionUrl",
                        "validateDistributionUrl",
                        "zipStoreBase",
                        "zipStorePath"
                );
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
