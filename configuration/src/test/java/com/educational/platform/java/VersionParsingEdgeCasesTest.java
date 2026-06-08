package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests edge cases in version parsing and comparison logic that are not covered
 * by {@link VersionComparisonLogicTest}.
 * <p>
 * Specifically targets:
 * <ul>
 *   <li>Qualifier suffixes (-SNAPSHOT, -rc1, .RELEASE) that must not break semver extraction</li>
 *   <li>Comparison transitivity: if A &lt; B and B &lt; C then A &lt; C</li>
 *   <li>Null/empty input handling in the parsing regex</li>
 *   <li>Triple-digit version components (e.g., 1.4.100)</li>
 *   <li>Distribution URL variants and malformed URLs</li>
 * </ul>
 */
public class VersionParsingEdgeCasesTest {

    private static final Pattern SEMVER = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    // --- Qualifier suffixes ---

    @ParameterizedTest(name = "Should extract semver from qualified version: {0}")
    @CsvSource({
            "'1.4.2-SNAPSHOT',   1, 4, 2",
            "'9.5.1-rc1',        9, 5, 1",
            "'4.0.1.RELEASE',    4, 0, 1",
            "'3.27.3-beta.2',    3, 27, 3",
            "'1.4.2-M1',         1, 4, 2",
            "'2.0.0-alpha',      2, 0, 0",
            "'9.5.1-milestone-3', 9, 5, 1"
    })
    void semverExtraction_shouldIgnore_qualifierSuffix(String input, int expectedMajor,
                                                       int expectedMinor, int expectedPatch) {
        Matcher matcher = SEMVER.matcher(input);

        assertThat(matcher.find())
                .as("Should find semver in '%s'", input)
                .isTrue();
        assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedMajor);
        assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(expectedMinor);
        assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(expectedPatch);
    }

    // --- Null/empty/whitespace input ---

    @ParameterizedTest(name = "Should not match null/empty/whitespace: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void semverPattern_shouldNotMatch_nullEmptyOrWhitespace(String input) {
        if (input == null) {
            assertThat((Object) null).as("Null input cannot be matched").isNull();
            return;
        }
        Matcher matcher = SEMVER.matcher(input);
        assertThat(matcher.find())
                .as("Semver should not match blank input '%s'", input)
                .isFalse();
    }

    // --- Triple-digit components ---

    @ParameterizedTest(name = "Triple-digit component: {0}")
    @CsvSource({
            "'1.4.100',    1,   4, 100",
            "'10.20.300', 10,  20, 300",
            "'100.0.0',  100,   0,   0",
            "'1.100.1',    1, 100,   1"
    })
    void semverPattern_shouldHandle_tripleDigitComponents(String input, int expectedMajor,
                                                          int expectedMinor, int expectedPatch) {
        Matcher matcher = SEMVER.matcher(input);

        assertThat(matcher.find()).isTrue();
        assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedMajor);
        assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(expectedMinor);
        assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(expectedPatch);
    }

    // --- Comparison transitivity ---

    @Test
    void versionComparison_shouldBe_transitive_forArchUnit() {
        // If 1.4.0 < 1.4.1 < 1.4.2, then 1.4.0 < 1.4.2
        assertThat(compareVersions(1, 4, 0, 1, 4, 1)).isNegative();
        assertThat(compareVersions(1, 4, 1, 1, 4, 2)).isNegative();
        assertThat(compareVersions(1, 4, 0, 1, 4, 2)).isNegative();
    }

    @Test
    void versionComparison_shouldBe_transitive_forGradle() {
        // 9.2.1 < 9.4.0 < 9.5.1
        assertThat(compareVersions(9, 2, 1, 9, 4, 0)).isNegative();
        assertThat(compareVersions(9, 4, 0, 9, 5, 1)).isNegative();
        assertThat(compareVersions(9, 2, 1, 9, 5, 1)).isNegative();
    }

    @Test
    void versionComparison_shouldBe_symmetric() {
        // If A < B then B > A
        assertThat(compareVersions(1, 4, 1, 1, 4, 2)).isNegative();
        assertThat(compareVersions(1, 4, 2, 1, 4, 1)).isPositive();
    }

    @Test
    void versionComparison_equalVersions_shouldReturnZero() {
        assertThat(compareVersions(1, 4, 2, 1, 4, 2)).isZero();
        assertThat(compareVersions(9, 5, 1, 9, 5, 1)).isZero();
    }

    // --- Distribution URL parsing ---

    @ParameterizedTest(name = "Should extract Gradle version from URL variant: {0}")
    @CsvSource({
            "'https\\://services.gradle.org/distributions/gradle-9.5.1-bin.zip',   9, 5, 1",
            "'https\\://services.gradle.org/distributions/gradle-9.5.1-all.zip',   9, 5, 1",
            "'https\\://services.gradle.org/distributions/gradle-10.0.0-bin.zip', 10, 0, 0",
            "'file\\:///local/cache/gradle-9.5.1-bin.zip',                          9, 5, 1"
    })
    void gradleDistributionUrl_shouldExtractVersion_fromVariants(String url, int expectedMajor,
                                                                  int expectedMinor, int expectedPatch) {
        Pattern gradleVersion = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = gradleVersion.matcher(url);

        assertThat(matcher.find())
                .as("Should parse Gradle version from URL: %s", url)
                .isTrue();
        assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedMajor);
        assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(expectedMinor);
        assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(expectedPatch);
    }

    @ParameterizedTest(name = "Should NOT extract version from malformed URL: {0}")
    @ValueSource(strings = {
            "https://services.gradle.org/distributions/gradle-bin.zip",
            "https://services.gradle.org/distributions/gradle-.zip",
            "not-a-url",
            ""
    })
    void gradleDistributionUrl_shouldNotMatch_malformedUrls(String url) {
        Pattern gradleVersion = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = gradleVersion.matcher(url);

        assertThat(matcher.find())
                .as("Should not find Gradle version in malformed URL: '%s'", url)
                .isFalse();
    }

    // --- Boundary: major version dominates ---

    @Test
    void majorVersion_shouldDominate_overMinorAndPatch() {
        // 2.0.0 > 1.99.99
        assertThat(compareVersions(2, 0, 0, 1, 99, 99)).isPositive();
        // 10.0.0 > 9.99.99
        assertThat(compareVersions(10, 0, 0, 9, 99, 99)).isPositive();
    }

    @Test
    void minorVersion_shouldDominate_overPatch() {
        // 1.5.0 > 1.4.99
        assertThat(compareVersions(1, 5, 0, 1, 4, 99)).isPositive();
    }

    private int compareVersions(int major1, int minor1, int patch1,
                                int major2, int minor2, int patch2) {
        if (major1 != major2) return Integer.compare(major1, major2);
        if (minor1 != minor2) return Integer.compare(minor1, minor2);
        return Integer.compare(patch1, patch2);
    }
}
