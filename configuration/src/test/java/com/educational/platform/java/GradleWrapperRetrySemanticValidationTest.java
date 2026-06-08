package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the semantic correctness of the Gradle wrapper retry/backoff
 * properties added during the Gradle 9.2.1 → 9.5.1 upgrade.
 * <p>
 * Gradle 9.5.1 introduced {@code retries} and {@code retryBackOffMs} to
 * control distribution download resilience. These properties have semantic
 * constraints beyond their raw values:
 * <ul>
 *   <li>{@code retries} must be a non-negative integer</li>
 *   <li>{@code retryBackOffMs} must be a positive integer (even if retries=0)</li>
 *   <li>{@code networkTimeout} must be positive and larger than retryBackOffMs</li>
 *   <li>All numeric properties must parse without overflow</li>
 * </ul>
 * <p>
 * {@link GradleWrapperPropertiesCompletenessTest} validates property existence
 * and exact values. {@link GradleWrapperConfigTest} validates wrapper version.
 * This test validates <em>semantic relationships</em> between properties.
 */
public class GradleWrapperRetrySemanticValidationTest {

    @Test
    void retries_shouldBe_nonNegative() throws IOException {
        Properties props = readWrapperProperties();
        int retries = Integer.parseInt(props.getProperty("retries"));

        assertThat(retries)
                .as("retries must be non-negative (negative retries are nonsensical)")
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void retryBackOffMs_shouldBe_positive() throws IOException {
        Properties props = readWrapperProperties();
        int backOff = Integer.parseInt(props.getProperty("retryBackOffMs"));

        assertThat(backOff)
                .as("retryBackOffMs must be positive (zero/negative backoff defeats retry purpose)")
                .isGreaterThan(0);
    }

    @Test
    void networkTimeout_shouldExceed_retryBackOffMs() throws IOException {
        Properties props = readWrapperProperties();
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));
        int backOff = Integer.parseInt(props.getProperty("retryBackOffMs"));

        assertThat(timeout)
                .as("networkTimeout (%dms) should exceed retryBackOffMs (%dms)", timeout, backOff)
                .isGreaterThan(backOff);
    }

    @Test
    void retryBackOffMs_shouldBe_lessThanOneMinute() throws IOException {
        Properties props = readWrapperProperties();
        int backOff = Integer.parseInt(props.getProperty("retryBackOffMs"));

        assertThat(backOff)
                .as("retryBackOffMs should be less than 60000ms (1 minute) to avoid excessive delays")
                .isLessThan(60_000);
    }

    @Test
    void networkTimeout_shouldBe_between1And60Seconds() throws IOException {
        Properties props = readWrapperProperties();
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));

        assertThat(timeout)
                .as("networkTimeout should be at least 1000ms (1 second)")
                .isGreaterThanOrEqualTo(1000)
                .as("networkTimeout should not exceed 60000ms (1 minute)")
                .isLessThanOrEqualTo(60_000);
    }

    @ParameterizedTest(name = "Numeric property ''{0}'' should parse without overflow")
    @ValueSource(strings = {"retries", "retryBackOffMs", "networkTimeout"})
    void numericProperty_shouldFit_inInteger(String key) throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty(key);

        assertThat(value).as("Property '%s' should exist", key).isNotNull();

        long parsed = Long.parseLong(value);
        assertThat(parsed)
                .as("Property '%s' value %d should fit in int range", key, parsed)
                .isBetween((long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE);
    }

    @Test
    void totalMaxWaitTime_shouldBe_reasonable() throws IOException {
        Properties props = readWrapperProperties();
        int retries = Integer.parseInt(props.getProperty("retries"));
        int backOff = Integer.parseInt(props.getProperty("retryBackOffMs"));
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));

        // Worst case: each retry hits network timeout + backoff
        long maxWaitMs = (long) (retries + 1) * timeout + (long) retries * backOff;

        assertThat(maxWaitMs)
                .as("Total maximum wait time (%dms) should be under 5 minutes", maxWaitMs)
                .isLessThanOrEqualTo(5 * 60 * 1000L);
    }

    @Test
    void retriesZero_means_noRetryAttempts() throws IOException {
        Properties props = readWrapperProperties();
        int retries = Integer.parseInt(props.getProperty("retries"));

        assertThat(retries)
                .as("With retries=0, Gradle should attempt download exactly once (fail-fast for CI)")
                .isEqualTo(0);
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
