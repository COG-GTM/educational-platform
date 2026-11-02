package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-2-bom.version} override: jackson-databind below 2.21.4 is affected by CVE-2026-54513.
 */
class JacksonDatabindVersionTest {

    private static final Version MINIMUM = new Version(2, 21, 4, null, "com.fasterxml.jackson.core", "jackson-databind");

    @Test
    void jacksonDatabindIsPatchedForCve202654513() {
        final Version resolved = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        assertTrue(resolved.compareTo(MINIMUM) >= 0,
                () -> "jackson-databind " + resolved + " is below " + MINIMUM + " (CVE-2026-54513)");
    }
}
