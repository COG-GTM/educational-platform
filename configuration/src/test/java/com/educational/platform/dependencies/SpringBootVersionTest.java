package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Boot below 4.0.6 is affected by CVE-2026-40975 (weak PRNG, CWE-338).
 */
class SpringBootVersionTest {

    private static final int[] MINIMUM_VERSION = {4, 0, 6};

    @Test
    void springBootVersion_isNotVulnerableToCve202640975() {
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version is not available on the classpath");

        String[] parts = version.split("[.-]");
        int comparison = 0;
        for (int i = 0; i < MINIMUM_VERSION.length && comparison == 0; i++) {
            comparison = Integer.compare(Integer.parseInt(parts[i]), MINIMUM_VERSION[i]);
        }

        assertTrue(comparison >= 0, "Spring Boot must be >= 4.0.6 (CVE-2026-40975), but was " + version);
    }
}
