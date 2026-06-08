package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the version comparison logic used throughout the Java 26 upgrade tests.
 * <p>
 * Several tests in this package compare semver triples to enforce minimum
 * version requirements (e.g., ArchUnit >= 1.4.2, Gradle >= 9.4.0).
 * This test validates the comparison logic itself against edge cases to prevent
 * false positives in the regression guards.
 */
public class VersionComparisonLogicTest {

    // --- ArchUnit >= 1.4.2 check ---

    @ParameterizedTest(name = "ArchUnit {0}.{1}.{2} meets Java 26 minimum ({3}): {4}")
    @CsvSource({
            "1, 4, 2, '1.4.2 — exact minimum',  true",
            "1, 4, 3, '1.4.3 — above minimum',   true",
            "1, 4, 10, '1.4.10 — double-digit patch', true",
            "1, 5, 0, '1.5.0 — higher minor',    true",
            "2, 0, 0, '2.0.0 — higher major',    true",
            "1, 4, 1, '1.4.1 — just below',      false",
            "1, 4, 0, '1.4.0 — below',           false",
            "1, 3, 9, '1.3.9 — lower minor',     false",
            "0, 9, 9, '0.9.9 — lower major',     false"
    })
    void archUnitMinimumVersionCheck(int major, int minor, int patch,
                                     String description, boolean expected) {
        boolean meetsMinimum = isArchUnitCompatibleWithJava26(major, minor, patch);

        assertThat(meetsMinimum)
                .as("ArchUnit %d.%d.%d — %s", major, minor, patch, description)
                .isEqualTo(expected);
    }

    // --- Gradle >= 9.4.0 check ---

    @ParameterizedTest(name = "Gradle {0}.{1}.{2} supports Java 26 ({3}): {4}")
    @CsvSource({
            "9, 4, 0, '9.4.0 — exact minimum',     true",
            "9, 4, 1, '9.4.1 — above minimum',      true",
            "9, 5, 1, '9.5.1 — project version',    true",
            "9, 99, 0, '9.99.0 — high minor',       true",
            "10, 0, 0, '10.0.0 — next major',       true",
            "9, 3, 9, '9.3.9 — just below minor',   false",
            "9, 3, 0, '9.3.0 — below minor',        false",
            "9, 2, 1, '9.2.1 — pre-upgrade version', false",
            "8, 99, 99, '8.99.99 — old major',      false"
    })
    void gradleMinimumVersionCheck(int major, int minor, int patch,
                                   String description, boolean expected) {
        boolean compatible = isGradleCompatibleWithJava26(major, minor, patch);

        assertThat(compatible)
                .as("Gradle %d.%d.%d — %s", major, minor, patch, description)
                .isEqualTo(expected);
    }

    // --- Semver parsing edge cases ---

    @ParameterizedTest(name = "Semver pattern should match: \"{0}\"")
    @CsvSource({
            "'1.4.2',     1, 4, 2",
            "'9.5.1',     9, 5, 1",
            "'10.0.0',   10, 0, 0",
            "'0.0.1',     0, 0, 1",
            "'99.99.99', 99, 99, 99"
    })
    void semverPattern_shouldParse_validVersions(String input, int expectedMajor,
                                                  int expectedMinor, int expectedPatch) {
        Pattern semver = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = semver.matcher(input);

        assertThat(matcher.find()).as("Pattern should match '%s'", input).isTrue();
        assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(expectedMajor);
        assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(expectedMinor);
        assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(expectedPatch);
    }

    @ParameterizedTest(name = "Semver pattern should not match: \"{0}\"")
    @CsvSource(value = {
            "''",
            "'abc'",
            "'1.2'",
            "'x.y.z'",
            "'.1.2.3'",
    })
    void semverPattern_shouldNotMatch_invalidVersions(String input) {
        Pattern semver = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
        Matcher matcher = semver.matcher(input);

        assertThat(matcher.find())
                .as("Pattern should not match '%s'", input)
                .isFalse();
    }

    // --- Gradle distributionUrl parsing ---

    @Test
    void gradleDistributionUrl_shouldBe_parseable() {
        String url = "https\\://services.gradle.org/distributions/gradle-9.5.1-bin.zip";
        Pattern pattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = pattern.matcher(url);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("9");
        assertThat(matcher.group(2)).isEqualTo("5");
        assertThat(matcher.group(3)).isEqualTo("1");
    }

    @Test
    void gradleDistributionUrl_withOldVersion_shouldBeParseable() {
        String url = "https\\://services.gradle.org/distributions/gradle-9.2.1-bin.zip";
        Pattern pattern = Pattern.compile("gradle-(\\d+)\\.(\\d+)\\.(\\d+)");
        Matcher matcher = pattern.matcher(url);

        assertThat(matcher.find()).isTrue();
        assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(9);
        assertThat(Integer.parseInt(matcher.group(2))).isEqualTo(2);
        assertThat(Integer.parseInt(matcher.group(3))).isEqualTo(1);

        boolean compatible = isGradleCompatibleWithJava26(9, 2, 1);
        assertThat(compatible).as("Old Gradle 9.2.1 should NOT be compatible with Java 26").isFalse();
    }

    // --- Class file major version formula ---

    @ParameterizedTest(name = "Java {0} → class file major version {1}")
    @CsvSource({
            "21, 65",
            "22, 66",
            "23, 67",
            "24, 68",
            "25, 69",
            "26, 70"
    })
    void classFileMajorVersion_formula_shouldHold(int javaVersion, int expectedMajor) {
        int computed = 44 + javaVersion;
        assertThat(computed).isEqualTo(expectedMajor);
    }

    private boolean isArchUnitCompatibleWithJava26(int major, int minor, int patch) {
        return (major > 1)
                || (major == 1 && minor > 4)
                || (major == 1 && minor == 4 && patch >= 2);
    }

    private boolean isGradleCompatibleWithJava26(int major, int minor, int patch) {
        if (major > 9) return true;
        if (major < 9) return false;
        return minor >= 4;
    }
}
