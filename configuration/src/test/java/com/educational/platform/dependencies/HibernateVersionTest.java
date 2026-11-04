package com.educational.platform.dependencies;

import org.hibernate.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2020-25638 / SNYK-JAVA-ORGHIBERNATEORM-5926269 (SQL injection), fixed in hibernate-core 6.2.7.Final.
 */
class HibernateVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {6, 2, 7};

    @Test
    void hibernateCoreIsPatchedForCve202025638() {
        // given
        String hibernateVersion = Version.getVersionString();

        // when
        int[] version = parseVersion(hibernateVersion);

        // then
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "hibernate-core " + hibernateVersion + " is vulnerable to CVE-2020-25638; require >= 6.2.7.Final");
    }

    @Test
    void parseVersion_extractsNumericPartsAndIgnoresQualifier() {
        // given
        String version = "7.2.0.Final";

        // when
        int[] parsedVersion = parseVersion(version);
        int[] parsedMinimumVersion = parseVersion("6.2.7.Final");

        // then
        assertArrayEquals(new int[]{7, 2, 0}, parsedVersion, "should parse numeric components before qualifier");
        assertArrayEquals(new int[]{6, 2, 7}, parsedMinimumVersion, "should parse the minimum safe version");
    }

    @Test
    void parseVersion_handlesMultiDigitComponents() {
        // given
        String version = "6.10.12.Final";

        // when
        int[] parsedVersion = parseVersion(version);

        // then
        assertArrayEquals(new int[]{6, 10, 12}, parsedVersion, "should preserve multi-digit components");
    }

    @Test
    void parseVersion_padsMissingComponentsWithZero() {
        // given
        String version = "6.2";

        // when
        int[] parsedVersion = parseVersion(version);

        // then
        assertArrayEquals(new int[]{6, 2, 0}, parsedVersion, "should pad missing patch version with zero");
    }

    @Test
    void parseVersion_handlesDashQualifierInPatch() {
        // given
        String version = "6.2.7-SNAPSHOT";

        // when
        int[] parsedVersion = parseVersion(version);

        // then
        assertArrayEquals(new int[]{6, 2, 7}, parsedVersion, "should ignore a dash qualifier in the patch component");
    }

    @Test
    void isAtLeast_acceptsExactMinimum() {
        // given
        int[] version = {6, 2, 7};

        // when
        boolean atLeastMinimum = isAtLeast(version, MINIMUM_SAFE_VERSION);

        // then
        assertTrue(atLeastMinimum, "the exact minimum safe version should be accepted");
    }

    @Test
    void isAtLeast_rejectsVersionsBelowMinimum() {
        // given
        int[] belowByPatch = {6, 2, 6};
        int[] belowByMinor = {6, 1, 99};
        int[] belowByMajor = {5, 6, 15};

        // when
        boolean patchVersionAccepted = isAtLeast(belowByPatch, MINIMUM_SAFE_VERSION);
        boolean minorVersionAccepted = isAtLeast(belowByMinor, MINIMUM_SAFE_VERSION);
        boolean majorVersionAccepted = isAtLeast(belowByMajor, MINIMUM_SAFE_VERSION);

        // then
        assertFalse(patchVersionAccepted, "a patch version below the minimum should be rejected");
        assertFalse(minorVersionAccepted, "a minor version below the minimum should be rejected");
        assertFalse(majorVersionAccepted, "a major version below the minimum should be rejected");
    }

    @Test
    void isAtLeast_acceptsNewerVersions() {
        // given
        int[] newerPatch = {6, 2, 8};
        int[] newerMultiDigitMinor = {6, 10, 0};
        int[] newerMajor = {7, 0, 0};
        int[] newerMajorAndMinor = {7, 2, 0};

        // when
        boolean patchAccepted = isAtLeast(newerPatch, MINIMUM_SAFE_VERSION);
        boolean multiDigitMinorAccepted = isAtLeast(newerMultiDigitMinor, MINIMUM_SAFE_VERSION);
        boolean majorAccepted = isAtLeast(newerMajor, MINIMUM_SAFE_VERSION);
        boolean majorAndMinorAccepted = isAtLeast(newerMajorAndMinor, MINIMUM_SAFE_VERSION);

        // then
        assertTrue(patchAccepted, "a newer patch version should be accepted");
        assertTrue(multiDigitMinorAccepted, "a newer multi-digit minor version should be compared numerically");
        assertTrue(majorAccepted, "a newer major version should be accepted");
        assertTrue(majorAndMinorAccepted, "a newer major and minor version should be accepted");
    }

    @Test
    void isAtLeast_rejectsUnparseableVersion() {
        // given
        int[] version = parseVersion("unknown");

        // when
        boolean atLeastMinimum = isAtLeast(version, MINIMUM_SAFE_VERSION);

        // then
        assertFalse(atLeastMinimum, "an unparseable version should fail closed");
    }

    @Test
    void hibernateVersionString_isPresentAndNumeric() {
        // given
        String version = Version.getVersionString();

        // when
        boolean isNumericVersion = version != null && version.matches("\\d+\\.\\d+\\.\\d+.*");

        // then
        assertTrue(version != null, "Hibernate version string should be present");
        assertTrue(isNumericVersion, "Hibernate version string should begin with three numeric components");
    }

    static int[] parseVersion(String versionString) {
        String[] parts = versionString.split("\\.");
        int[] version = {0, 0, 0};
        for (int i = 0; i < version.length && i < parts.length; i++) {
            version[i] = leadingInt(parts[i]);
        }
        return version;
    }

    private static int leadingInt(String value) {
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(value.substring(0, end));
    }

    static boolean isAtLeast(int[] actual, int[] minimum) {
        for (int i = 0; i < minimum.length; i++) {
            if (actual[i] != minimum[i]) {
                return actual[i] > minimum[i];
            }
        }
        return true;
    }
}
