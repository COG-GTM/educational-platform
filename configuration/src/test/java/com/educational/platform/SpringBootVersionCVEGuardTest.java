package com.educational.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing to a Spring Boot release affected by CVE-2026-40971
 * (SNYK-JAVA-ORGSPRINGFRAMEWORKBOOT-16200231, certificate host mismatch in spring-boot-autoconfigure).
 */
class SpringBootVersionCVEGuardTest {

    @Test
    void springBootVersion_isAtLeast_4_0_6() {
        // given
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version must be available on the classpath");

        // when
        String[] parts = version.split("[.-]");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        // then
        boolean patched = major > 4 || (major == 4 && (minor > 0 || patch >= 6));
        assertTrue(patched, "Spring Boot " + version + " is affected by CVE-2026-40971; use 4.0.6 or later");
    }
}
