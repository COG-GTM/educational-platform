package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private static void assertNotAffected(String version) {
        assertNotNull(version, "Spring Security version could not be resolved");
        final int[] actual = Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();
        for (int[][] range : AFFECTED_RANGES) {
            assertFalse(Arrays.compare(actual, range[0]) >= 0 && Arrays.compare(actual, range[1]) <= 0,
                    "Spring Security " + version + " is affected by CVE-2024-22257");
        }
    }
}
