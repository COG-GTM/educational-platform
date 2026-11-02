package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.core.json.PackageVersion;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-bom.version} override in the root build script
 * (GHSA-72hv-8253-57qq / SNYK-JAVA-TOOLSJACKSONCORE-15365915, fixed in 3.1.0).
 */
class JacksonCoreVersionTest {

    @Test
    void jacksonCore_isAtLeastPatchedVersion() {
        final Version resolved = PackageVersion.VERSION;

        final boolean patched = resolved.getMajorVersion() > 3
                || (resolved.getMajorVersion() == 3 && resolved.getMinorVersion() >= 1);

        assertTrue(patched, "tools.jackson.core:jackson-core " + resolved
                + " is vulnerable to GHSA-72hv-8253-57qq; expected >= 3.1.0");
    }
}
