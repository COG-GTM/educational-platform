package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2024-22257 (CWE-639): affects Spring Security 5.7.0-5.7.11, 5.8.0-5.8.10, 6.0.0-6.0.8,
 * 6.1.0-6.1.7 and 6.2.0-6.2.2.
 */
class SpringSecurityWebVersionTest {

    private static final int[][][] AFFECTED_RANGES = {
            {{5, 7, 0}, {5, 7, 11}},
            {{5, 8, 0}, {5, 8, 10}},
            {{6, 0, 0}, {6, 0, 8}},
            {{6, 1, 0}, {6, 1, 7}},
            {{6, 2, 0}, {6, 2, 2}},
    };

    @Test
    void springSecurityWeb_isNotAffectedByCve202422257() {
        assertNotAffected(SecurityFilterChain.class.getPackage().getImplementationVersion());
    }

    @Test
    void springSecurityCore_isNotAffectedByCve202422257() {
        assertNotAffected(SpringSecurityCoreVersion.getVersion());
    }

    @Test
    void affectedRangeBoundaries_areFlagged() {
        assertAll(
                () -> assertTrue(isAffected("5.7.0"), "5.7.0 should be affected"),
                () -> assertTrue(isAffected("5.7.11"), "5.7.11 should be affected"),
                () -> assertTrue(isAffected("5.8.0"), "5.8.0 should be affected"),
                () -> assertTrue(isAffected("5.8.10"), "5.8.10 should be affected"),
                () -> assertTrue(isAffected("6.0.0"), "6.0.0 should be affected"),
                () -> assertTrue(isAffected("6.0.8"), "6.0.8 should be affected"),
                () -> assertTrue(isAffected("6.1.0"), "6.1.0 should be affected"),
                () -> assertTrue(isAffected("6.1.7"), "6.1.7 should be affected"),
                () -> assertTrue(isAffected("6.2.0"), "6.2.0 should be affected"),
                () -> assertTrue(isAffected("6.2.2"), "6.2.2 should be affected")
        );
    }

    @Test
    void fixedVersions_areNotFlagged() {
        assertAll(
                () -> assertFalse(isAffected("5.7.12"), "5.7.12 should not be affected"),
                () -> assertFalse(isAffected("5.8.11"), "5.8.11 should not be affected"),
                () -> assertFalse(isAffected("6.0.9"), "6.0.9 should not be affected"),
                () -> assertFalse(isAffected("6.1.8"), "6.1.8 should not be affected"),
                () -> assertFalse(isAffected("6.2.3"), "6.2.3 should not be affected"),
                () -> assertFalse(isAffected("6.3.0"), "6.3.0 should not be affected"),
                () -> assertFalse(isAffected("7.0.2"), "7.0.2 should not be affected")
        );
    }

    @Test
    void qualifiedAndShortVersions_areParsedOnNumericPrefix() {
        assertAll(
                () -> assertTrue(isAffected("6.1.7-SNAPSHOT"), "6.1.7-SNAPSHOT should be affected"),
                () -> assertFalse(isAffected("6.1.8-SNAPSHOT"), "6.1.8-SNAPSHOT should not be affected"),
                () -> assertFalse(isAffected("7.0.0-M1"), "7.0.0-M1 should not be affected"),
                () -> assertTrue(isAffected("6.2"), "6.2 should be affected"),
                () -> assertFalse(isAffected("7"), "7 should not be affected")
        );
    }

    @Test
    void unparseableVersion_failsLoudly() {
        assertThrows(NumberFormatException.class, () -> isAffected("unknown"));
    }

    @Test
    void springSecurityWebAndCore_resolveToSameVersion() {
        assertEquals(SpringSecurityCoreVersion.getVersion(),
                SecurityFilterChain.class.getPackage().getImplementationVersion());
    }

    static int[] parse(String version) {
        final String[] components = version.split("[.-]");
        final int[] parsed = new int[3];
        for (int i = 0; i < Math.min(components.length, parsed.length); i++) {
            parsed[i] = Integer.parseInt(components[i]);
        }
        return parsed;
    }

    static boolean isAffected(String version) {
        final int[] actual = parse(version);
        return Arrays.stream(AFFECTED_RANGES)
                .anyMatch(range -> Arrays.compare(actual, range[0]) >= 0
                        && Arrays.compare(actual, range[1]) <= 0);
    }

    private static void assertNotAffected(String version) {
        assertNotNull(version, "Spring Security version could not be resolved");
        assertFalse(isAffected(version), "Spring Security " + version + " is affected by CVE-2024-22257");
    }
}
