package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the security and network configuration of the Gradle wrapper
 * properties after the Gradle 9.2.1 to 9.5.1 upgrade.
 * <p>
 * The wrapper properties file controls how Gradle is downloaded and verified.
 * This test ensures the distribution uses HTTPS, the official domain, a known
 * distribution type, and sensible network timeout/retry values.
 */
public class GradleWrapperSecurityConfigTest {

    @Test
    void distributionUrl_shouldUse_https() throws IOException {
        String distUrl = readDistributionUrl();

        assertThat(distUrl)
                .as("Distribution URL should use HTTPS for secure download")
                .containsPattern("https\\\\?://");
    }

    @Test
    void distributionUrl_shouldUse_officialGradleDomain() throws IOException {
        String distUrl = readDistributionUrl();

        assertThat(distUrl)
                .as("Distribution URL should use the official Gradle services domain")
                .contains("services.gradle.org");
    }

    @Test
    void distributionUrl_shouldUse_binDistributionType() throws IOException {
        String distUrl = readDistributionUrl();

        assertThat(distUrl)
                .as("Distribution URL should use -bin.zip (binary-only, not -all.zip with sources)")
                .contains("-bin.zip");
    }

    @Test
    void networkTimeout_shouldBe_reasonable() throws IOException {
        Properties props = readWrapperProperties();
        String networkTimeout = props.getProperty("networkTimeout");

        assertThat(networkTimeout)
                .as("networkTimeout should be defined")
                .isNotNull();

        int timeoutMs = Integer.parseInt(networkTimeout);
        assertThat(timeoutMs)
                .as("networkTimeout should be between 5000ms and 60000ms")
                .isBetween(5000, 60000);
    }

    @Test
    void retries_shouldBe_nonNegative() throws IOException {
        Properties props = readWrapperProperties();
        String retries = props.getProperty("retries");

        assertThat(retries)
                .as("retries should be defined")
                .isNotNull();

        int retriesCount = Integer.parseInt(retries);
        assertThat(retriesCount)
                .as("retries should be a non-negative integer")
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void retryBackOffMs_shouldBe_positive() throws IOException {
        Properties props = readWrapperProperties();
        String retryBackOffMs = props.getProperty("retryBackOffMs");

        assertThat(retryBackOffMs)
                .as("retryBackOffMs should be defined")
                .isNotNull();

        int backoffMs = Integer.parseInt(retryBackOffMs);
        assertThat(backoffMs)
                .as("retryBackOffMs should be a positive value")
                .isPositive();
    }

    @Test
    void validateDistributionUrl_shouldBeEnabled() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("validateDistributionUrl"))
                .as("validateDistributionUrl should be true for security")
                .isEqualTo("true");
    }

    @Test
    void distributionBase_shouldBe_gradleUserHome() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("distributionBase"))
                .as("distributionBase should be GRADLE_USER_HOME")
                .isEqualTo("GRADLE_USER_HOME");
    }

    @Test
    void zipStoreBase_shouldBe_gradleUserHome() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("zipStoreBase"))
                .as("zipStoreBase should be GRADLE_USER_HOME")
                .isEqualTo("GRADLE_USER_HOME");
    }

    private Properties readWrapperProperties() throws IOException {
        Path propsFile = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(propsFile));
        return props;
    }

    private String readDistributionUrl() throws IOException {
        return readWrapperProperties().getProperty("distributionUrl");
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
