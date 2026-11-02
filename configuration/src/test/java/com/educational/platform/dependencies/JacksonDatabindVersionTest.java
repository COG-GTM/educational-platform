package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * jackson-databind below 2.21.4 is vulnerable to CVE-2026-54512 (GHSA-j3rv-43j4-c7qm).
 */
class JacksonDatabindVersionTest {

    private static final Version MINIMUM_SAFE_VERSION = new Version(2, 21, 4, null, null, null);

    @Test
    void jacksonDatabind_isPatchedForCve202654512() {
        final Version resolved = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        assertTrue(resolved.compareTo(MINIMUM_SAFE_VERSION) >= 0,
                "jackson-databind " + resolved + " must be >= " + MINIMUM_SAFE_VERSION + " (CVE-2026-54512)");
    }
}
