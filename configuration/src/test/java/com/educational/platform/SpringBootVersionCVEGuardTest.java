package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Spring Boot version against regression below 4.0.7, which remediates
 * CVE-2026-40971, CVE-2026-40973, CVE-2026-40974, CVE-2026-40975, CVE-2026-40976,
 * CVE-2026-40977, CVE-2026-40992 and CVE-2026-41001.
 */
class SpringBootVersionCVEGuardTest {

    private static final int REQUIRED_MAJOR = 4;
    private static final int REQUIRED_MINOR = 0;
    private static final int REQUIRED_PATCH = 7;

    @Test
    void springBootVersion_isAtLeast_4_0_7() {
        // given
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version must be available to verify the CVE guard");

        // when
        String[] parts = version.split("[.-]");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        // then
        boolean atLeastRequired =
                major > REQUIRED_MAJOR
                        || (major == REQUIRED_MAJOR && minor > REQUIRED_MINOR)
                        || (major == REQUIRED_MAJOR && minor == REQUIRED_MINOR && patch >= REQUIRED_PATCH);
        assertTrue(atLeastRequired,
                "Spring Boot version must be at least 4.0.7 to remediate known CVEs, but was: " + version);
    }
}
