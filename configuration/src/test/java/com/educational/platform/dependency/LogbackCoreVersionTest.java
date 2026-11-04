package com.educational.platform.dependency;

import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2023-6378 (logback receiver deserialization DoS) is fixed in logback-core 1.4.12.
 */
class LogbackCoreVersionTest {

    private static final int[] MIN_FIXED_VERSION = {1, 4, 12};

    @Test
    void logbackCore_resolvedVersion_isAtLeastFixedVersion() {
        final String version = Appender.class.getPackage().getImplementationVersion();
        assertNotNull(version, "logback-core version could not be determined");
        assertTrue(compare(version, MIN_FIXED_VERSION) >= 0,
                "logback-core " + version + " is below " + Arrays.toString(MIN_FIXED_VERSION));
    }

    @Test
    void compare_exactFixedVersion_isAccepted() {
        assertEquals(0, compare("1.4.12", MIN_FIXED_VERSION));
    }

    @Test
    void compare_olderPatchVersion_isRejectedNumerically() {
        assertAll(
                () -> assertTrue(compare("1.4.7", MIN_FIXED_VERSION) < 0),
                () -> assertTrue(compare("1.4.11", MIN_FIXED_VERSION) < 0));
    }

    @Test
    void compare_olderMajorOrMinorVersion_isRejectedDespiteHigherRemainingComponents() {
        assertAll(
                () -> assertTrue(compare("0.99.99", MIN_FIXED_VERSION) < 0),
                () -> assertTrue(compare("1.3.99", MIN_FIXED_VERSION) < 0));
    }

    @Test
    void compare_newerVersion_isAcceptedByFirstDifferentComponent() {
        assertAll(
                () -> assertTrue(compare("1.4.13", MIN_FIXED_VERSION) > 0),
                () -> assertTrue(compare("1.5.0", MIN_FIXED_VERSION) > 0),
                () -> assertTrue(compare("1.10.0", MIN_FIXED_VERSION) > 0),
                () -> assertTrue(compare("2.0.0", MIN_FIXED_VERSION) > 0));
    }

    @Test
    void compare_emptyOrIncompleteVersion_doesNotMeetMinimum() {
        assertAll(
                () -> assertTrue(compare("", MIN_FIXED_VERSION) < 0),
                () -> assertTrue(compare("1", MIN_FIXED_VERSION) < 0),
                () -> assertTrue(compare("1.4", MIN_FIXED_VERSION) < 0),
                () -> assertTrue(compare("1.4.invalid", MIN_FIXED_VERSION) < 0));
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
