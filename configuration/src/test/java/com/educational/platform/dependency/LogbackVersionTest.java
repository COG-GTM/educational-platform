package com.educational.platform.dependency;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2026-13006 (expression injection) is fixed in logback 1.5.36.
 */
class LogbackVersionTest {

    private static final int[] MIN_FIXED_VERSION = {1, 5, 36};

    @Test
    void logbackCore_resolvedVersion_isAtLeastFixedVersion() {
        assertAtLeastFixedVersion("logback-core", Appender.class);
    }

    @Test
    void logbackClassic_resolvedVersion_isAtLeastFixedVersion() {
        assertAtLeastFixedVersion("logback-classic", Logger.class);
    }

    private static void assertAtLeastFixedVersion(String artifact, Class<?> clazz) {
        final String version = clazz.getPackage().getImplementationVersion();
        assertNotNull(version, artifact + " version could not be determined");
        assertTrue(compare(version, MIN_FIXED_VERSION) >= 0,
                artifact + " " + version + " is below " + Arrays.toString(MIN_FIXED_VERSION));
    }

    private static int compare(String version, int[] minimum) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            final int part = i < parts.length && parts[i].matches("\\d+") ? Integer.parseInt(parts[i]) : 0;
            if (part != minimum[i]) {
                return Integer.compare(part, minimum[i]);
            }
        }
        return 0;
    }
}
