package com.educational.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.SpringSecurityCoreVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing below Spring Security 7.0.5, which fixes CVE-2026-22754.
 */
class SpringSecurityVersionGuardTest {

    private static final int[] MINIMUM = {7, 0, 5};

    @Test
    void springSecurityConfig_isAtLeast_7_0_5() {
        assertAtLeastMinimum("spring-security-config", HttpSecurity.class.getPackage().getImplementationVersion());
    }

    @Test
    void springSecurityCore_isAtLeast_7_0_5() {
        assertAtLeastMinimum("spring-security-core", SpringSecurityCoreVersion.getVersion());
    }

    private static void assertAtLeastMinimum(String artifact, String version) {
        assertNotNull(version, artifact + " version must be available");

        String[] parts = version.split("[.-]");
        int comparison = 0;
        for (int i = 0; i < MINIMUM.length && comparison == 0; i++) {
            comparison = Integer.compare(Integer.parseInt(parts[i]), MINIMUM[i]);
        }

        assertTrue(comparison >= 0, artifact + " must be at least 7.0.5 (CVE-2026-22754), but was: " + version);
    }
}
