package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the structure and security properties of the Gradle wrapper
 * distribution URL after the 9.2.1 → 9.5.1 upgrade.
 * <p>
 * The distribution URL must:
 * <ul>
 *   <li>Use HTTPS (not HTTP) for integrity</li>
 *   <li>Point to the official Gradle distribution server</li>
 *   <li>Use the {@code -bin} distribution type (not {@code -all} or {@code -src})</li>
 *   <li>End with {@code .zip}</li>
 *   <li>Contain a parseable semver version that is Java 26-compatible</li>
 * </ul>
 */
public class GradleDistributionUrlFormatTest {

    private static final Pattern DISTRIBUTION_URL_PATTERN = Pattern.compile(
            "https://services\\.gradle\\.org/distributions/gradle-(\\d+\\.\\d+\\.\\d+)-(bin|all)\\.zip"
    );

    @Test
    void distributionUrl_shouldUse_httpsProtocol() throws IOException {
        String url = readDistributionUrl();

        assertThat(url)
                .as("Distribution URL should use HTTPS")
                .startsWith("https");
    }

    @Test
    void distributionUrl_shouldPointTo_officialGradleServer() throws IOException {
        String url = readDistributionUrl();

        assertThat(url)
                .as("Distribution URL should use services.gradle.org")
                .contains("services.gradle.org/distributions/");
    }

    @Test
    void distributionUrl_shouldUse_binDistributionType() throws IOException {
        String url = readDistributionUrl();

        assertThat(url)
                .as("Distribution URL should use -bin type (smaller, faster download)")
                .contains("-bin.zip");

        assertThat(url)
                .as("Distribution URL should not use -src type")
                .doesNotContain("-src.zip");
    }

    @Test
    void distributionUrl_shouldEndWith_zipExtension() throws IOException {
        String url = readDistributionUrl();

        assertThat(url)
                .as("Distribution URL should end with .zip")
                .endsWith(".zip");
    }

    @Test
    void distributionUrl_shouldMatchExpectedFormat() throws IOException {
        String url = readDistributionUrl();
        Matcher matcher = DISTRIBUTION_URL_PATTERN.matcher(url);

        assertThat(matcher.matches())
                .as("Distribution URL '%s' should match expected format: https://services.gradle.org/distributions/gradle-X.Y.Z-bin.zip", url)
                .isTrue();
    }

    @Test
    void distributionUrl_version_shouldBe_951() throws IOException {
        String url = readDistributionUrl();
        Matcher matcher = Pattern.compile("gradle-(\\d+\\.\\d+\\.\\d+)").matcher(url);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1))
                .as("Gradle version in distribution URL")
                .isEqualTo("9.5.1");
    }

    @Test
    void distributionUrl_shouldNotContain_snapshotOrRcSuffix() throws IOException {
        String url = readDistributionUrl();

        assertThat(url)
                .as("Distribution URL should not reference a snapshot build")
                .doesNotContainIgnoringCase("snapshot")
                .doesNotContainIgnoringCase("-rc")
                .doesNotContainIgnoringCase("-milestone");
    }

    @ParameterizedTest(name = "Distribution URL should not reference old version: {0}")
    @ValueSource(strings = {"9.2.1", "9.3.0", "9.4.0", "8.0.0"})
    void distributionUrl_shouldNotReference_olderVersions(String oldVersion) throws IOException {
        String url = readDistributionUrl();

        assertThat(url)
                .as("Distribution URL should not reference Gradle %s", oldVersion)
                .doesNotContain("gradle-" + oldVersion);
    }

    @Test
    void distributionUrl_rawFile_shouldContain_escapedColon() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        String rawContent = Files.readString(wrapperProps);

        // Gradle wrapper properties escape ':' as '\:' in the raw file;
        // Properties.load() unescapes this. Verify the raw format is correct.
        assertThat(rawContent)
                .as("Raw properties file should contain escaped colon in distribution URL")
                .contains("https\\:");
    }

    private String readDistributionUrl() throws IOException {
        Path wrapperProps = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        props.load(Files.newInputStream(wrapperProps));
        return props.getProperty("distributionUrl");
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
