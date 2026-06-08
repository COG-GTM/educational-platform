package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the interaction between the Gradle wrapper retry and timeout properties
 * added in the Gradle 9.5.1 upgrade.
 * <p>
 * The three properties {@code networkTimeout}, {@code retries}, and
 * {@code retryBackOffMs} work together to define the download behavior:
 * <pre>
 *   Total max wait ≈ (retries + 1) × networkTimeout + retries × retryBackOffMs
 * </pre>
 * <p>
 * {@link GradleWrapperRetrySemanticValidationTest} validates semantic bounds.
 * {@link GradleWrapperSecurityConfigTest} validates network/security settings.
 * This test validates the <em>exact values</em> declared in the project and
 * their combined effect using concrete calculations.
 */
public class GradleRetryAndTimeoutInteractionTest {

    @Test
    void projectRetryConfig_exactValues() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("retries"))
                .as("retries should be exactly '0' (no automatic retries)")
                .isEqualTo("0");

        assertThat(props.getProperty("retryBackOffMs"))
                .as("retryBackOffMs should be exactly '500'")
                .isEqualTo("500");

        assertThat(props.getProperty("networkTimeout"))
                .as("networkTimeout should be exactly '10000'")
                .isEqualTo("10000");
    }

    @Test
    void totalMaxWaitTime_withZeroRetries_shouldEqual_singleTimeout() throws IOException {
        Properties props = readWrapperProperties();
        int retries = Integer.parseInt(props.getProperty("retries"));
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));
        int backoff = Integer.parseInt(props.getProperty("retryBackOffMs"));

        // Formula: (retries + 1) * timeout + retries * backoff
        long totalMaxWaitMs = (long) (retries + 1) * timeout + (long) retries * backoff;

        assertThat(retries)
                .as("With 0 retries, there should be exactly 1 attempt")
                .isEqualTo(0);

        assertThat(totalMaxWaitMs)
                .as("With 0 retries, total max wait = 1 × networkTimeout = %d ms", timeout)
                .isEqualTo(timeout);
    }

    @ParameterizedTest(name = "retries={0}, timeout={1}, backoff={2} → totalMaxWait={3}")
    @CsvSource({
            "0, 10000, 500, 10000",
            "1, 10000, 500, 20500",
            "2, 10000, 500, 31000",
            "3, 10000, 500, 41500",
            "0,  5000, 100,  5000",
            "5, 10000, 1000, 65000"
    })
    void totalMaxWaitTime_formula_shouldBeCorrect(int retries, int timeout, int backoff,
                                                   long expectedTotal) {
        long totalMaxWaitMs = (long) (retries + 1) * timeout + (long) retries * backoff;

        assertThat(totalMaxWaitMs)
                .as("Total max wait for retries=%d, timeout=%d, backoff=%d",
                        retries, timeout, backoff)
                .isEqualTo(expectedTotal);
    }

    @Test
    void totalMaxWaitTime_shouldBe_underFiveMinutes() throws IOException {
        Properties props = readWrapperProperties();
        int retries = Integer.parseInt(props.getProperty("retries"));
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));
        int backoff = Integer.parseInt(props.getProperty("retryBackOffMs"));

        long totalMaxWaitMs = (long) (retries + 1) * timeout + (long) retries * backoff;
        long fiveMinutesMs = 5 * 60 * 1000L;

        assertThat(totalMaxWaitMs)
                .as("Total max wait time should be under 5 minutes (300000 ms)")
                .isLessThan(fiveMinutesMs);
    }

    @Test
    void backoff_shouldBe_lessThan_networkTimeout() throws IOException {
        Properties props = readWrapperProperties();
        int timeout = Integer.parseInt(props.getProperty("networkTimeout"));
        int backoff = Integer.parseInt(props.getProperty("retryBackOffMs"));

        assertThat(backoff)
                .as("retryBackOffMs (%d) should be less than networkTimeout (%d)", backoff, timeout)
                .isLessThan(timeout);
    }

    @Test
    void retryConfig_shouldBeConsistent_withDefaultNoRetryStrategy() throws IOException {
        Properties props = readWrapperProperties();
        int retries = Integer.parseInt(props.getProperty("retries"));

        // With retries=0, the backoff value is effectively unused but must be valid
        if (retries == 0) {
            int backoff = Integer.parseInt(props.getProperty("retryBackOffMs"));
            assertThat(backoff)
                    .as("Even with 0 retries, retryBackOffMs should be a sensible default (positive)")
                    .isPositive();
        }
    }

    @Test
    void allThreeRetryProperties_shouldBe_numericIntegers() throws IOException {
        Properties props = readWrapperProperties();

        for (String key : RETRY_PROPERTY_KEYS) {
            String value = props.getProperty(key);
            assertThat(value)
                    .as("Property '%s' should be defined", key)
                    .isNotNull();
            assertThat(value)
                    .as("Property '%s' value '%s' should be a valid integer", key, value)
                    .matches("\\d+");
        }
    }

    private static final java.util.List<String> RETRY_PROPERTY_KEYS = java.util.List.of(
            "networkTimeout", "retries", "retryBackOffMs");

    private Properties readWrapperProperties() throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties")));
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
