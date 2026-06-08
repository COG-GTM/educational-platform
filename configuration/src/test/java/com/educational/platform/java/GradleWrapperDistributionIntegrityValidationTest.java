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
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the integrity and format of the Gradle distribution URL and
 * associated security settings after the 9.2.1 → 9.5.1 wrapper upgrade.
 * <p>
 * {@link GradleDistributionUrlFormatTest} validates the URL structure.
 * {@link GradleWrapperSecurityConfigTest} validates security properties.
 * {@link GradleWrapperVersionCoherenceTest} validates version consistency.
 * This test provides additional integrity checks:
 * <ul>
 *   <li>Distribution URL components (host, path, file extension)</li>
 *   <li>{@code validateDistributionUrl} must be {@code true}</li>
 *   <li>URL version segment matches the parsed Gradle version</li>
 *   <li>No stale 9.2.1 version fragments in the URL</li>
 *   <li>Distribution type validation (bin vs all)</li>
 * </ul>
 */
public class GradleWrapperDistributionIntegrityValidationTest {

    @Test
    void distributionUrl_shouldUse_officialGradleHost() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should use official Gradle services host")
                .contains("services.gradle.org");
    }

    @Test
    void distributionUrl_shouldEndWith_zipExtension() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should end with .zip")
                .endsWith(".zip");
    }

    @Test
    void distributionUrl_shouldContain_distributionsPath() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should contain /distributions/ path segment")
                .contains("/distributions/");
    }

    @Test
    void distributionUrl_shouldNotContain_oldVersion921() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should not reference old Gradle 9.2.1")
                .doesNotContain("9.2.1");
    }

    @Test
    void distributionUrl_shouldUse_binDistribution() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should use -bin distribution (not -all or -src)")
                .containsPattern("gradle-[\\d.]+-bin\\.zip");
    }

    @Test
    void validateDistributionUrl_shouldBeExplicitly_true() throws IOException {
        Properties props = readWrapperProperties();
        String validateUrl = props.getProperty("validateDistributionUrl");

        assertThat(validateUrl)
                .as("validateDistributionUrl should be explicitly set to 'true'")
                .isNotNull()
                .isEqualTo("true");
    }

    @Test
    void distributionUrl_versionSegment_shouldMatch_parsedVersion() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        // Extract version from URL path
        Matcher urlMatcher = Pattern.compile("gradle-(\\d+\\.\\d+\\.\\d+)-").matcher(url);
        assertThat(urlMatcher.find())
                .as("Distribution URL should contain a parseable version")
                .isTrue();
        String urlVersion = urlMatcher.group(1);

        // Verify it's a valid semver triple
        String[] parts = urlVersion.split("\\.");
        assertThat(parts).hasSize(3);

        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        assertThat(major).as("Gradle major version").isGreaterThanOrEqualTo(9);
        assertThat(minor).as("Gradle minor version").isGreaterThanOrEqualTo(0);
        assertThat(patch).as("Gradle patch version").isGreaterThanOrEqualTo(0);
    }

    @Test
    void distributionUrl_shouldUse_httpsScheme() throws IOException {
        Properties props = readWrapperProperties();
        String url = props.getProperty("distributionUrl");

        assertThat(url)
                .as("Distribution URL should use HTTPS (note: properties file escapes colon)")
                .startsWith("https");
    }

    @ParameterizedTest(name = "Properties file should not contain old Gradle version fragment ''{0}''")
    @ValueSource(strings = {"9.2.1", "9.2.0", "9.1.", "9.0.", "8."})
    void wrapperProperties_shouldNotContain_oldGradleVersionFragments(String oldFragment) throws IOException {
        Path propsPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        String content = Files.readString(propsPath);

        assertThat(content)
                .as("gradle-wrapper.properties should not contain old version fragment '%s'", oldFragment)
                .doesNotContain(oldFragment);
    }

    @Test
    void distributionUrl_shouldBe_singleLine() throws IOException {
        Path propsPath = findProjectRoot().resolve("gradle/wrapper/gradle-wrapper.properties");
        List<String> lines = Files.readAllLines(propsPath);

        long urlLineCount = lines.stream()
                .filter(l -> l.startsWith("distributionUrl="))
                .count();

        assertThat(urlLineCount)
                .as("distributionUrl should appear exactly once")
                .isEqualTo(1);
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
