package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void isAtLeast_rejectsVulnerableVersions() {
        assertAll(
                () -> assertFalse(isAtLeast("0.0.0", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("2.7.18", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("3.0.13", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("3.1.0", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("3.1.4", MINIMUM_SAFE_VERSION))
        );
    }

    @Test
    void isAtLeast_acceptsMinimumSafeVersion() {
        assertTrue(isAtLeast("3.1.5", MINIMUM_SAFE_VERSION));
    }

    @Test
    void isAtLeast_acceptsNewerPatchMinorAndMajorVersions() {
        assertAll(
                () -> assertTrue(isAtLeast("3.1.6", MINIMUM_SAFE_VERSION)),
                () -> assertTrue(isAtLeast("3.2.0", MINIMUM_SAFE_VERSION)),
                () -> assertTrue(isAtLeast("4.0.1", MINIMUM_SAFE_VERSION))
        );
    }

    @Test
    void isAtLeast_comparesVersionComponentsNumerically() {
        assertAll(
                () -> assertTrue(isAtLeast("3.1.10", MINIMUM_SAFE_VERSION)),
                () -> assertTrue(isAtLeast("3.10.0", "3.9.9")),
                () -> assertTrue(isAtLeast("10.0.0", "9.9.9")),
                () -> assertFalse(isAtLeast("3.1.5", "3.1.10")),
                () -> assertFalse(isAtLeast("3.9.9", "3.10.0")),
                () -> assertFalse(isAtLeast("9.9.9", "10.0.0"))
        );
    }

    @Test
    void isAtLeast_treatsMissingComponentsAsZero() {
        assertAll(
                () -> assertTrue(isAtLeast("4", "4.0.0")),
                () -> assertTrue(isAtLeast("3.2", "3.2.0")),
                () -> assertTrue(isAtLeast("3.2.0", "3.2")),
                () -> assertFalse(isAtLeast("3.1", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("3", MINIMUM_SAFE_VERSION))
        );
    }

    @Test
    void isAtLeast_comparesNumericComponentsOfQualifiedVersions() {
        assertAll(
                () -> assertTrue(isAtLeast("4.0.1-SNAPSHOT", MINIMUM_SAFE_VERSION)),
                () -> assertTrue(isAtLeast("3.2.0.RELEASE", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("3.1.4-SNAPSHOT", MINIMUM_SAFE_VERSION)),
                () -> assertFalse(isAtLeast("3.1.0.RELEASE", MINIMUM_SAFE_VERSION))
        );
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
