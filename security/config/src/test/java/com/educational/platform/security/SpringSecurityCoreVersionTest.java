package com.educational.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.SpringSecurityCoreVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against resolving spring-security-core below 6.1.2, the first 6.1.x release fixing CVE-2023-34034.
 */
class SpringSecurityCoreVersionTest {

    private static final int[] MINIMUM = {6, 1, 2};

    @Test
    void springSecurityCore_isNotVulnerableToCve_2023_34034() {
        String version = SpringSecurityCoreVersion.getVersion();
        assertNotNull(version, "spring-security-core version must be available");

        String[] parts = version.split("[.-]");
        int comparison = 0;
        for (int i = 0; i < MINIMUM.length && comparison == 0; i++) {
            comparison = Integer.compare(Integer.parseInt(parts[i]), MINIMUM[i]);
        }

        assertTrue(comparison >= 0, "spring-security-core must be at least 6.1.2 (CVE-2023-34034), but was: " + version);
    }
}
