package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-2-bom.version} override in the root build script
 * (GHSA-72hv-8253-57qq / SNYK-JAVA-COMFASTERXMLJACKSONCORE-15365924, fixed in 2.21.1).
 */
class JacksonCoreVersionTest {

    private static final Version MINIMUM = new Version(2, 21, 1, null, "com.fasterxml.jackson.core", "jackson-core");

    @Test
    void jacksonCore_isAtLeastPatchedVersion() {
        final Version resolved = PackageVersion.VERSION;

        assertTrue(resolved.compareTo(MINIMUM) >= 0,
                "jackson-core " + resolved + " is vulnerable to GHSA-72hv-8253-57qq; expected >= " + MINIMUM);
    }
}
