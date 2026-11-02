package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot 4.0.6 is the first 4.0.x release fixing CVE-2026-40974 (certificate host mismatch).
 */
class SpringBootVersionTest {

    private static final int[] MINIMUM_VERSION = {4, 0, 6};

    @Test
    void springBootVersion_isNotVulnerableToCve202640974() {
        // given
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version must be resolvable from the classpath");

        // when
        String[] parts = version.split("[.-]");

        // then
        int comparison = 0;
        for (int i = 0; i < MINIMUM_VERSION.length && comparison == 0; i++) {
            comparison = Integer.compare(Integer.parseInt(parts[i]), MINIMUM_VERSION[i]);
        }
        assertTrue(comparison >= 0, "Spring Boot must be at least 4.0.6 (CVE-2026-40974), but was " + version);
    }
}
