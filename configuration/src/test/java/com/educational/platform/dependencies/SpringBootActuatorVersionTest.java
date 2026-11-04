package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2023-20883 (spring-boot-actuator-autoconfigure before 3.1.5).
 */
class SpringBootActuatorVersionTest {

    private static final String MINIMUM_SAFE_VERSION = "3.1.5";
    private static final String ACTUATOR_AUTOCONFIGURE_CLASS =
            "org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration";

    @Test
    void springBootVersion_shouldNotBeVulnerableToCve202320883() {
        String version = SpringBootVersion.getVersion();

        assertNotNull(version, "Spring Boot version could not be determined");
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "Spring Boot " + version + " is vulnerable to CVE-2023-20883; upgrade to " + MINIMUM_SAFE_VERSION + " or later");
    }

    @Test
    void actuatorAutoconfigure_whenPresent_shouldNotBeVulnerableToCve202320883() {
        Class<?> actuatorClass;
        try {
            actuatorClass = Class.forName(ACTUATOR_AUTOCONFIGURE_CLASS, false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            return;
        }

        String version = actuatorClass.getPackage().getImplementationVersion();
        assertNotNull(version, "spring-boot-actuator-autoconfigure version could not be determined");
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "spring-boot-actuator-autoconfigure " + version + " is vulnerable to CVE-2023-20883; upgrade to " + MINIMUM_SAFE_VERSION + " or later");
    }

    static boolean isAtLeast(String actual, String minimum) {
        int[] a = numericParts(actual);
        int[] m = numericParts(minimum);
        for (int i = 0; i < Math.max(a.length, m.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < m.length ? m[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return true;
    }

    private static int[] numericParts(String version) {
        return Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(part -> part.matches("\\d+") ? Integer.parseInt(part) : 0)
                .toArray();
    }
}
