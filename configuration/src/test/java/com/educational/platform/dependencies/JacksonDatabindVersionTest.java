package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.cfg.PackageVersion;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-2-bom.version} override: jackson-databind below 2.15.3 is affected by
 * CVE-2023-35116 (SNYK-JAVA-COMFASTERXMLJACKSONCORE-6096293, unbounded recursion DoS).
 */
class JacksonDatabindVersionTest {

    private static final Version MINIMUM = new Version(2, 15, 3, null, null, null);

    // Version.compareTo orders by groupId/artifactId first, so compare the numeric parts only.
    private static final Comparator<Version> BY_NUMBER = Comparator
            .comparingInt(Version::getMajorVersion)
            .thenComparingInt(Version::getMinorVersion)
            .thenComparingInt(Version::getPatchLevel);

    @Test
    void jacksonDatabind_isAtLeastFixedVersion() {
        Version actual = PackageVersion.VERSION;
        assertTrue(BY_NUMBER.compare(actual, MINIMUM) >= 0, () -> actual + " must be >= " + MINIMUM);
    }
}
