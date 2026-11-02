package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2026-40992 (SNYK-JAVA-ORGSPRINGFRAMEWORKBOOT-17304741) is fixed in spring-boot-autoconfigure 4.0.7.
 */
class SpringBootVersionTest {

    private static final int[] MINIMUM_VERSION = {4, 0, 7};

    @Test
    void springBoot_isAtLeastMinimumPatchedVersion() {
        assertAtLeastMinimum(SpringBootVersion.getVersion());
    }

    @Test
    void springBootAutoconfigure_isAtLeastMinimumPatchedVersion() {
        assertAtLeastMinimum(SpringBootApplication.class.getPackage().getImplementationVersion());
    }

    private static void assertAtLeastMinimum(String version) {
        assertNotNull(version, "Spring Boot version must be resolvable");
        int[] actual = Arrays.stream(version.split("[.-]")).limit(3).mapToInt(Integer::parseInt).toArray();
        assertEquals(3, actual.length, "Unexpected Spring Boot version format: " + version);
        assertTrue(Arrays.compare(actual, MINIMUM_VERSION) >= 0,
                "Spring Boot must be >= 4.0.7 (CVE-2026-40992), but was " + version);
    }
}
