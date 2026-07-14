package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2026-40974 (improper validation of certificate with host mismatch)
 * in org.springframework.boot:spring-boot-autoconfigure, fixed in Spring Boot 4.0.6.
 */
class SpringBootVersionCVE202640974Test {

    @Test
    void springBootVersionIsNotVulnerableToCVE202640974() {
        String version = SpringBootVersion.getVersion();
        String[] parts = version.split("[.-]");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        boolean fixed = major > 4
                || (major == 4 && (minor > 0 || patch >= 6))
                || (major == 3 && (minor > 5 || (minor == 5 && patch >= 14)));

        assertTrue(fixed, "Spring Boot " + version + " is vulnerable to CVE-2026-40974; requires >= 4.0.6 or 3.5.14");
    }
}
