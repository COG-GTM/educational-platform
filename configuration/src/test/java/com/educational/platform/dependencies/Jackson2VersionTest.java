package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-2-bom.version} override: jackson-core below 2.21.2 is affected by
 * SNYK-JAVA-COMFASTERXMLJACKSONCORE-15907551 (CWE-770).
 */
class Jackson2VersionTest {

    private static final Version MINIMUM = new Version(2, 21, 2, null, null, null);

    // Version.compareTo orders by groupId/artifactId first, so compare the numeric parts only.
    private static final Comparator<Version> BY_NUMBER = Comparator
            .comparingInt(Version::getMajorVersion)
            .thenComparingInt(Version::getMinorVersion)
            .thenComparingInt(Version::getPatchLevel);

    @Test
    void jacksonCore_isAtLeastFixedVersion() {
        assertAtLeastMinimum(com.fasterxml.jackson.core.json.PackageVersion.VERSION);
    }

    @Test
    void jacksonDatabind_isAtLeastFixedVersion() {
        assertAtLeastMinimum(com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION);
    }

    private static void assertAtLeastMinimum(Version actual) {
        assertTrue(BY_NUMBER.compare(actual, MINIMUM) >= 0, () -> actual + " must be >= " + MINIMUM);
    }
}
