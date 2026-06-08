package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper properties file is complete and coherent
 * after the Gradle 9.2.1 → 9.5.1 upgrade.
 * <p>
 * Gradle 9.5.1 introduced new properties ({@code retries}, {@code retryBackOffMs})
 * that must be present for deterministic CI builds. This test ensures all required
 * properties are defined together — individual property tests exist elsewhere, but
 * this test treats the properties file as an atomic configuration unit.
 */
public class GradleWrapperPropertiesCompletenessTest {

    private static final Set<String> REQUIRED_PROPERTIES = Set.of(
            "distributionBase",
            "distributionPath",
            "distributionUrl",
            "networkTimeout",
            "retries",
            "retryBackOffMs",
            "validateDistributionUrl",
            "zipStoreBase",
            "zipStorePath"
    );

    @Test
    void wrapperProperties_shouldContain_allRequiredKeys() throws IOException {
        Properties props = readWrapperProperties();

        for (String key : REQUIRED_PROPERTIES) {
            assertThat(props.containsKey(key))
                    .as("Wrapper properties must contain key: %s", key)
                    .isTrue();
        }
    }

    @Test
    void wrapperProperties_shouldNotContain_unexpectedKeys() throws IOException {
        Properties props = readWrapperProperties();

        for (String key : props.stringPropertyNames()) {
            assertThat(REQUIRED_PROPERTIES)
                    .as("Unexpected property '%s' in wrapper properties", key)
                    .contains(key);
        }
    }

    @Test
    void wrapperProperties_shouldHave_exactlyExpectedNumberOfEntries() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.size())
                .as("Wrapper properties should have exactly %d entries", REQUIRED_PROPERTIES.size())
                .isEqualTo(REQUIRED_PROPERTIES.size());
    }

    @ParameterizedTest(name = "Property ''{0}'' should have non-empty value")
    @ValueSource(strings = {
            "distributionBase",
            "distributionPath",
            "distributionUrl",
            "networkTimeout",
            "retries",
            "retryBackOffMs",
            "validateDistributionUrl",
            "zipStoreBase",
            "zipStorePath"
    })
    void wrapperProperty_shouldHave_nonEmptyValue(String key) throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty(key))
                .as("Property '%s' should have a non-empty value", key)
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void distributionPath_shouldBe_wrapperDists() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("distributionPath"))
                .as("distributionPath should follow Gradle convention")
                .isEqualTo("wrapper/dists");
    }

    @Test
    void zipStorePath_shouldBe_wrapperDists() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("zipStorePath"))
                .as("zipStorePath should follow Gradle convention")
                .isEqualTo("wrapper/dists");
    }

    @Test
    void networkTimeout_shouldBe_10000ms() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("networkTimeout"))
                .as("networkTimeout should be 10000ms (10 seconds)")
                .isEqualTo("10000");
    }

    @Test
    void retries_shouldBe_zero() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("retries"))
                .as("retries should be 0 (fail fast in CI)")
                .isEqualTo("0");
    }

    @Test
    void retryBackOffMs_shouldBe_500() throws IOException {
        Properties props = readWrapperProperties();

        assertThat(props.getProperty("retryBackOffMs"))
                .as("retryBackOffMs should be 500ms")
                .isEqualTo("500");
    }

    @Test
    void numericProperties_shouldParseAsIntegers() throws IOException {
        Properties props = readWrapperProperties();

        String[] numericKeys = {"networkTimeout", "retries", "retryBackOffMs"};
        for (String key : numericKeys) {
            String value = props.getProperty(key);
            assertThat(value).isNotNull();
            try {
                int parsed = Integer.parseInt(value);
                assertThat(parsed)
                        .as("Property '%s' value '%s' should parse as non-negative integer", key, value)
                        .isGreaterThanOrEqualTo(0);
            } catch (NumberFormatException e) {
                throw new AssertionError(
                        "Property '" + key + "' value '" + value + "' should be a valid integer", e);
            }
        }
    }

    @Test
    void booleanProperties_shouldBeValidBoolean() throws IOException {
        Properties props = readWrapperProperties();

        String value = props.getProperty("validateDistributionUrl");
        assertThat(value)
                .as("validateDistributionUrl should be a valid boolean string")
                .isIn("true", "false");
    }

    @Test
    void wrapperPropertiesFile_shouldNotBeEmpty() throws IOException {
        Path propsFile = findWrapperPropertiesPath();
        long size = Files.size(propsFile);

        assertThat(size)
                .as("Wrapper properties file should not be empty")
                .isGreaterThan(0);
    }

    @Test
    void wrapperPropertiesFile_shouldEndWith_newline() throws IOException {
        Path propsFile = findWrapperPropertiesPath();
        byte[] bytes = Files.readAllBytes(propsFile);

        assertThat(bytes[bytes.length - 1])
                .as("Properties file should end with a newline")
                .isEqualTo((byte) '\n');
    }

    private Properties readWrapperProperties() throws IOException {
        Path propsFile = findWrapperPropertiesPath();
        Properties props = new Properties();
        props.load(Files.newInputStream(propsFile));
        return props;
    }

    private Path findWrapperPropertiesPath() {
        Path current = Paths.get(System.getProperty("user.dir"));
        while (current != null) {
            Path candidate = current.resolve("gradle/wrapper/gradle-wrapper.properties");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Paths.get("gradle/wrapper/gradle-wrapper.properties");
    }
}
