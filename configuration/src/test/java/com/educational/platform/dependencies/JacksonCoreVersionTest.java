package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Version actual = PackageVersion.VERSION;
        assertTrue(BY_NUMBER.compare(actual, MINIMUM) >= 0, () -> "jackson-core " + actual + " must be >= " + MINIMUM);
    }
}
