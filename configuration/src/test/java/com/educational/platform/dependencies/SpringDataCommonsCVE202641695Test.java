package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the spring-data-bom override pinning spring-data-commons to at least 4.0.6,
 * which fixes CVE-2026-41695, CVE-2026-41711, CVE-2026-41716 and CVE-2026-41721.
 */
class SpringDataCommonsCVE202641695Test {

    private static final int[] MINIMUM_VERSION = {4, 0, 6};

    @Test
    void resolvedSpringDataCommonsVersion_isAtLeast_4_0_6() {
        // given
        final String version = Pageable.class.getPackage().getImplementationVersion();

        // then
        assertNotNull(version, "spring-data-commons Implementation-Version should be present in the jar manifest");
        assertTrue(isAtLeastMinimum(version),
                "spring-data-commons version should be >= 4.0.6 to fix CVE-2026-41695/41711/41716/41721, but was " + version);
    }

    private static boolean isAtLeastMinimum(String version) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < MINIMUM_VERSION.length; i++) {
            final int part = i < parts.length ? parseNumeric(parts[i]) : 0;
            if (part != MINIMUM_VERSION[i]) {
                return part > MINIMUM_VERSION[i];
            }
        }
        return true;
    }

    private static int parseNumeric(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
