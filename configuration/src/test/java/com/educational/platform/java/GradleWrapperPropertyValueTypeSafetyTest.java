package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates type safety of all Gradle wrapper property values after the
 * 9.2.1 → 9.5.1 upgrade.
 * <p>
 * {@link GradleWrapperPropertiesCompletenessTest} verifies exact values.
 * {@link GradleWrapperRetrySemanticValidationTest} validates semantic ranges.
 * This test validates that each property value conforms to its expected
 * <em>data type</em>: boolean, non-negative integer, valid URI, or
 * non-empty path string. This catches corruption like a boolean property
 * set to "yes" instead of "true", or a numeric property containing
 * whitespace that would parse differently across Gradle versions.
 */
public class GradleWrapperPropertyValueTypeSafetyTest {

    @Test
    void validateDistributionUrl_shouldParseAs_boolean() throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty("validateDistributionUrl");

        assertThat(value)
                .as("validateDistributionUrl should be a canonical boolean string")
                .isIn("true", "false");

        boolean parsed = Boolean.parseBoolean(value);
        assertThat(parsed)
                .as("validateDistributionUrl should parse as boolean true")
                .isTrue();
    }

    @ParameterizedTest(name = "Numeric property ''{0}'' should contain only digits")
    @ValueSource(strings = {"networkTimeout", "retries", "retryBackOffMs"})
    void numericProperty_shouldContain_onlyDigits(String key) throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty(key);

        assertThat(value)
                .as("Property '%s' should exist", key)
                .isNotNull();

        assertThat(value)
                .as("Property '%s' value '%s' should contain only digits", key, value)
                .matches("\\d+");
    }

    @ParameterizedTest(name = "Numeric property ''{0}'' should not have leading zeros")
    @ValueSource(strings = {"networkTimeout", "retryBackOffMs"})
    void numericProperty_shouldNotHave_leadingZeros(String key) throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty(key);

        assertThat(value)
                .as("Property '%s' should not have leading zeros (except for bare '0')", key)
                .doesNotMatch("0\\d+");
    }

    @Test
    void distributionUrl_shouldParseAs_validUri() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThatCode(() -> {
            URI uri = URI.create(url);
            assertThat(uri.getScheme())
                    .as("Distribution URL scheme should be https")
                    .isEqualTo("https");
            assertThat(uri.getHost())
                    .as("Distribution URL should have a valid host")
                    .isNotNull()
                    .isNotBlank();
        }).as("distributionUrl should parse as a valid URI")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Path property ''{0}'' should be a valid relative path")
    @ValueSource(strings = {"distributionPath", "zipStorePath"})
    void pathProperty_shouldBeValid_relativePath(String key) throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty(key);

        assertThat(value)
                .as("Property '%s' should be a non-empty path", key)
                .isNotNull()
                .isNotBlank();

        assertThat(value)
                .as("Property '%s' should not start with / (should be relative)", key)
                .doesNotStartWith("/");

        assertThat(value)
                .as("Property '%s' should not contain spaces", key)
                .doesNotContain(" ");
    }

    @ParameterizedTest(name = "Base property ''{0}'' should be a Gradle user home reference")
    @ValueSource(strings = {"distributionBase", "zipStoreBase"})
    void baseProperty_shouldReference_gradleUserHome(String key) throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty(key);

        assertThat(value)
                .as("Property '%s' should reference GRADLE_USER_HOME", key)
                .isEqualTo("GRADLE_USER_HOME");
    }

    @Test
    void allPropertyValues_shouldNotContain_leadingOrTrailingWhitespace() throws IOException {
        Properties props = readWrapperProperties();

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            assertThat(value)
                    .as("Property '%s' value should not have leading/trailing whitespace", key)
                    .isEqualTo(value.trim());
        }
    }

    @Test
    void retries_shouldBeExactlyZero_asCanonicalString() throws IOException {
        Properties props = readWrapperProperties();
        String value = props.getProperty("retries");

        // "0" is valid, "00" or " 0" are not canonical
        assertThat(value)
                .as("retries=0 should be the canonical representation")
                .isEqualTo("0");
    }

    private Properties readWrapperProperties() throws IOException {
        Path propsPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(propsPath));
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
