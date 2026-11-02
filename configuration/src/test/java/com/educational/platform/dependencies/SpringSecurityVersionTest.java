package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.web.savedrequest.CookieRequestCache;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Security 7.0.6 fixes CVE-2026-41706 (open redirect in spring-security-web).
 */
class SpringSecurityVersionTest {

    private static final int[] MINIMUM_VERSION = {7, 0, 6};

    @Test
    void springSecurityCore_isAtLeastMinimumVersion() {
        assertAtLeastMinimum(SpringSecurityCoreVersion.getVersion());
    }

    @Test
    void springSecurityWeb_isAtLeastMinimumVersion() {
        assertAtLeastMinimum(CookieRequestCache.class.getPackage().getImplementationVersion());
    }

    private static void assertAtLeastMinimum(String version) {
        assertNotNull(version, "Spring Security version could not be resolved");
        final int[] actual = Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();
        assertTrue(Arrays.compare(actual, MINIMUM_VERSION) >= 0,
                "Spring Security " + version + " is affected by CVE-2026-41706; 7.0.6 or later is required");
    }
}
