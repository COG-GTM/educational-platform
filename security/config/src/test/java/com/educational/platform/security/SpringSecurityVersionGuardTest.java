package com.educational.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing below Spring Security 7.0.5, which fixes CVE-2026-22747 (spring-security-web
 * user impersonation) and CVE-2026-22754. The BOM override lives in the root build script.
 */
class SpringSecurityVersionGuardTest {

    private static final int[] MINIMUM = {7, 0, 5};

    @Test
    void springSecurityWeb_isAtLeast_7_0_5() {
        assertAtLeastMinimum("spring-security-web", SecurityFilterChain.class.getPackage().getImplementationVersion());
    }

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

        assertTrue(comparison >= 0,
                artifact + " must be at least 7.0.5 (CVE-2026-22747, CVE-2026-22754), but was: " + version);
    }
}
