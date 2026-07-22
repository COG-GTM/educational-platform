package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.security.core.SpringSecurityCoreVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Spring Security version override remediating CVE-2026-22732, CVE-2026-22747,
 * CVE-2026-41706, CVE-2026-22754, CVE-2026-22753, CVE-2026-22751 and CVE-2026-22746
 * (all fixed in Spring Security 7.0.6), and the Spring Framework 7.0.8 compatibility
 * override required to run Spring Security 7.0.6.
 */
class SpringSecurityVersionCVETest {

    private static final int[] MIN_SECURITY_VERSION = {7, 0, 6};
    private static final int[] MIN_FRAMEWORK_VERSION = {7, 0, 8};

    @Test
    void springSecurityVersion_isAtLeast706_cveFixed() {
        // given
        final String version = SpringSecurityCoreVersion.getVersion();

        // then
        assertThat(version).isNotNull();
        assertThat(isAtLeast(version, MIN_SECURITY_VERSION))
                .as("Spring Security version %s must be >= 7.0.6 (CVE-2026-22732 et al.)", version)
                .isTrue();
    }

    @Test
    void springFrameworkVersion_isAtLeast708_requiredForSecurity706() {
        // given
        final String version = SpringVersion.getVersion();

        // then
        assertThat(version).isNotNull();
        assertThat(isAtLeast(version, MIN_FRAMEWORK_VERSION))
                .as("Spring Framework version %s must be >= 7.0.8 (compatibility with Security 7.0.6)", version)
                .isTrue();
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
