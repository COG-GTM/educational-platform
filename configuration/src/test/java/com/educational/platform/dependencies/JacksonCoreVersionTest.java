package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * jackson-core below 2.15.3 is affected by SNYK-JAVA-COMFASTERXMLJACKSONCORE-6248036 (CWE-770).
 */
class JacksonCoreVersionTest {

    private static final Version MINIMUM = new Version(2, 15, 3, null, null, null);

    // Version.compareTo orders by groupId/artifactId first, so compare the numeric parts only.
    private static final Comparator<Version> BY_NUMBER = Comparator
            .comparingInt(Version::getMajorVersion)
            .thenComparingInt(Version::getMinorVersion)
            .thenComparingInt(Version::getPatchLevel);

    @Test
    void jacksonCore_isAtLeastFixedVersion() {
        // given
        Version actual = PackageVersion.VERSION;

        // when
        boolean fixed = isAtLeastFixedVersion(actual);

        // then
        assertTrue(fixed, () -> "jackson-core " + actual + " must be >= " + MINIMUM);
    }

    @Test
    void jacksonDatabind_isAlignedWithJacksonCore() {
        // given
        Version core = PackageVersion.VERSION;
        Version databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // when
        int comparison = BY_NUMBER.compare(core, databind);

        // then
        assertEquals(0, comparison, () -> "jackson-core " + core + " must align with jackson-databind " + databind);
    }

    @Test
    void versionGuard_acceptsOnlyFixedVersions() {
        // given
        Version vulnerablePatch = new Version(2, 15, 2, null, null, null);
        Version fixedPatch = new Version(2, 15, 3, null, null, null);
        Version olderMinor = new Version(2, 14, 9, null, null, null);
        Version olderMajor = new Version(1, 99, 99, null, null, null);
        Version newerMinor = new Version(2, 16, 0, null, null, null);
        Version newerMajor = new Version(3, 0, 0, null, null, null);

        // when / then
        assertFalse(isAtLeastFixedVersion(vulnerablePatch));
        assertTrue(isAtLeastFixedVersion(fixedPatch));
        assertFalse(isAtLeastFixedVersion(olderMinor));
        assertFalse(isAtLeastFixedVersion(olderMajor));
        assertTrue(isAtLeastFixedVersion(newerMinor));
        assertTrue(isAtLeastFixedVersion(newerMajor));
    }

    static boolean isAtLeastFixedVersion(Version version) {
        return BY_NUMBER.compare(version, MINIMUM) >= 0;
    }
}
