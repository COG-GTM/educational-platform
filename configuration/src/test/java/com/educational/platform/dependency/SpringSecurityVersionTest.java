package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.SpringSecurityCoreVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Security 7.0.5 fixes CVE-2026-22751 (TOCTOU race condition in spring-security-core).
 */
class SpringSecurityVersionTest {

    private static final int[] MIN_VERSION = {7, 0, 5};

    @Test
    void springSecurityCore_isPatchedForCve202622751() {
        final String version = SpringSecurityCoreVersion.getVersion();

        assertNotNull(version, "spring-security-core version must be resolvable");
        assertTrue(isAtLeast(version, MIN_VERSION),
                "spring-security-core " + version + " must be >= 7.0.5 (CVE-2026-22751)");
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            final int part = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            if (part != minimum[i]) {
                return part > minimum[i];
            }
        }
        return true;
    }
}
