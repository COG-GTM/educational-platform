package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot below 4.0.6 is affected by CVE-2026-40973 (insecure temporary file, CWE-377).
 */
class SpringBootVersionGuardTest {

    private static final int[] MINIMUM_VERSION = {4, 0, 6};

    @Test
    void springBootVersion_isAtLeastPatchedRelease() {
        // given
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version must be resolvable");

        // when
        String[] parts = version.split("[.-]");
        int[] actual = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};

        // then
        assertTrue(compare(actual, MINIMUM_VERSION) >= 0,
                "Spring Boot must be >= 4.0.6 (CVE-2026-40973), but was: " + version);
    }

    private static int compare(int[] left, int[] right) {
        for (int i = 0; i < left.length; i++) {
            if (left[i] != right[i]) {
                return Integer.compare(left[i], right[i]);
            }
        }
        return 0;
    }
}
