package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper properties file is well-formed at the
 * byte level after the 9.2.1 → 9.5.1 upgrade.
 * <p>
 * {@link GradleWrapperPropertiesCompletenessTest} validates key-value content.
 * {@link GradleWrapperRetrySemanticValidationTest} validates semantic relationships.
 * <p>
 * This test validates <em>encoding and byte-level</em> properties:
 * <ul>
 *   <li>No UTF-8 BOM (breaks {@link Properties#load} on some JVMs)</li>
 *   <li>Latin-1/ASCII compatible (Java Properties spec requires ISO-8859-1)</li>
 *   <li>No null bytes or binary content</li>
 *   <li>Proper line termination</li>
 *   <li>No trailing whitespace after values (can cause subtle URL parsing issues)</li>
 * </ul>
 */
public class GradleWrapperPropertiesNoBomTest {

    @Test
    void wrapperProperties_shouldNotHave_utf8Bom() throws IOException {
        byte[] bytes = Files.readAllBytes(findWrapperProperties());

        if (bytes.length >= 3) {
            boolean hasBom = (bytes[0] == (byte) 0xEF
                    && bytes[1] == (byte) 0xBB
                    && bytes[2] == (byte) 0xBF);
            assertThat(hasBom)
                    .as("gradle-wrapper.properties should not start with UTF-8 BOM")
                    .isFalse();
        }
    }

    @Test
    void wrapperProperties_shouldNotContain_nullBytes() throws IOException {
        byte[] bytes = Files.readAllBytes(findWrapperProperties());

        for (int i = 0; i < bytes.length; i++) {
            assertThat(bytes[i])
                    .as("Null byte found at position %d in wrapper properties", i)
                    .isNotEqualTo((byte) 0x00);
        }
    }

    @Test
    void wrapperProperties_shouldBe_asciiCompatible() throws IOException {
        byte[] bytes = Files.readAllBytes(findWrapperProperties());

        for (int i = 0; i < bytes.length; i++) {
            int unsigned = bytes[i] & 0xFF;
            assertThat(unsigned)
                    .as("Byte at position %d (0x%02X) should be ASCII (< 128)", i, unsigned)
                    .isLessThan(128);
        }
    }

    @Test
    void wrapperProperties_shouldEndWith_newline() throws IOException {
        byte[] bytes = Files.readAllBytes(findWrapperProperties());

        assertThat(bytes.length)
                .as("Properties file should not be empty")
                .isGreaterThan(0);

        byte lastByte = bytes[bytes.length - 1];
        assertThat(lastByte == '\n' || lastByte == '\r')
                .as("Properties file should end with a newline character")
                .isTrue();
    }

    @ParameterizedTest(name = "Property value for ''{0}'' should not have trailing whitespace")
    @ValueSource(strings = {
            "distributionUrl",
            "distributionBase",
            "distributionPath",
            "networkTimeout",
            "retries",
            "retryBackOffMs",
            "validateDistributionUrl",
            "zipStoreBase",
            "zipStorePath"
    })
    void propertyValue_shouldNotHave_trailingWhitespace(String key) throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(findWrapperProperties()));

        String value = props.getProperty(key);
        if (value != null) {
            assertThat(value)
                    .as("Value of '%s' should not have trailing whitespace", key)
                    .isEqualTo(value.trim());
        }
    }

    @Test
    void wrapperProperties_lineCount_shouldBe_reasonable() throws IOException {
        String content = Files.readString(findWrapperProperties(), StandardCharsets.ISO_8859_1);
        long lineCount = content.lines().count();

        assertThat(lineCount)
                .as("Properties file should have a reasonable number of lines (9 properties + possible comments)")
                .isBetween(7L, 25L);
    }

    @Test
    void wrapperProperties_shouldNotContain_commentedOutProperties() throws IOException {
        String content = Files.readString(findWrapperProperties(), StandardCharsets.ISO_8859_1);

        long commentedPropertyCount = content.lines()
                .filter(line -> line.startsWith("#") || line.startsWith("!"))
                .filter(line -> line.contains("="))
                .count();

        assertThat(commentedPropertyCount)
                .as("Properties file should not have commented-out key=value pairs (dead config)")
                .isZero();
    }

    private Path findWrapperProperties() {
        return findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
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
