package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.SpringSecurityCoreVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CVE-2026-22753 (access control bypass in spring-security-config) is fixed in Spring Security 7.0.5.
 */
class SpringSecurityVersionTest {

    private static final int[] MIN_FIXED_VERSION = {7, 0, 5};

    @Test
    void springSecurityCore_isAtLeastFixedVersion() {
        assertAtLeastFixedVersion(SpringSecurityCoreVersion.getVersion());
    }

    @Test
    void springSecurityConfig_isAtLeastFixedVersion() {
        assertAtLeastFixedVersion(EnableWebSecurity.class.getPackage().getImplementationVersion());
    }

    private static void assertAtLeastFixedVersion(String version) {
        assertThat(version).isNotNull();
        assertThat(isAtLeast(version))
                .as("Spring Security %s must be >= 7.0.5 (CVE-2026-22753)", version)
                .isTrue();
    }

    private static boolean isAtLeast(String version) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < MIN_FIXED_VERSION.length; i++) {
            final int part = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            if (part != MIN_FIXED_VERSION[i]) {
                return part > MIN_FIXED_VERSION[i];
            }
        }
        return true;
    }
}
