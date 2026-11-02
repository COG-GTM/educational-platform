package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2026-40977 (SNYK-JAVA-ORGSPRINGFRAMEWORKBOOT-16201011, symlink attack) is fixed in spring-boot 4.0.6.
 */
class SpringBootVersionTest {

    private static final int[] MINIMUM_VERSION = {4, 0, 6};

    @Test
    void springBoot_isAtLeastMinimumPatchedVersion() {
        String version = SpringBootVersion.getVersion();
        assertNotNull(version, "Spring Boot version must be resolvable");
        int[] actual = Arrays.stream(version.split("[.-]")).limit(3).mapToInt(Integer::parseInt).toArray();
        assertEquals(3, actual.length, "Unexpected Spring Boot version format: " + version);
        assertTrue(Arrays.compare(actual, MINIMUM_VERSION) >= 0,
                "Spring Boot must be >= 4.0.6 (CVE-2026-40977), but was " + version);
    }
}
