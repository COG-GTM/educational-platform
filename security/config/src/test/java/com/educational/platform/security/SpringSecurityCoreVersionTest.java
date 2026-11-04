package com.educational.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertTrue(isAtLeast(version, MINIMUM),
                "spring-security-core must be at least 6.1.2 (CVE-2023-34034), but was: " + version);
    }

    @Test
    void springSecurityModules_resolveToSameVersionAsCore() {
        String coreVersion = SpringSecurityCoreVersion.getVersion();

        assertEquals(coreVersion, implementationVersion(HttpSecurity.class), "spring-security-config");
        assertEquals(coreVersion, implementationVersion(SecurityFilterChain.class), "spring-security-web");
        assertEquals(coreVersion, implementationVersion(PasswordEncoder.class), "spring-security-crypto");
    }

    @Test
    void isAtLeast_rejectsVersionsBelowMinimum() {
        assertFalse(isAtLeast("6.1.1", MINIMUM));
        assertFalse(isAtLeast("6.1.0", MINIMUM));
        assertFalse(isAtLeast("6.0.99", MINIMUM));
        assertFalse(isAtLeast("5.8.16", MINIMUM));
        assertFalse(isAtLeast("6.1", MINIMUM));
    }

    @Test
    void isAtLeast_acceptsMinimumAndNewerVersions() {
        assertTrue(isAtLeast("6.1.2", MINIMUM));
        assertTrue(isAtLeast("6.1.10", MINIMUM));
        assertTrue(isAtLeast("6.2.0", MINIMUM));
        assertTrue(isAtLeast("7.0.2", MINIMUM));
        assertTrue(isAtLeast("7.0", MINIMUM));
    }

    @Test
    void isAtLeast_ignoresQualifierSuffix() {
        assertTrue(isAtLeast("6.1.2-SNAPSHOT", MINIMUM));
        assertFalse(isAtLeast("6.1.1-SNAPSHOT", MINIMUM));
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            int actual = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            if (actual != minimum[i]) {
                return actual > minimum[i];
            }
        }
        return true;
    }

    private static String implementationVersion(Class<?> type) {
        String version = type.getPackage().getImplementationVersion();
        assertNotNull(version, type.getName() + " jar must declare Implementation-Version");
        return version;
    }
}
