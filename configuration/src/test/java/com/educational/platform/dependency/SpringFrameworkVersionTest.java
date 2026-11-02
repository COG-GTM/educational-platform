package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing to a Spring Framework version affected by CVE-2026-22740
 * (SNYK-JAVA-ORGSPRINGFRAMEWORK-16109615), fixed in 7.0.7.
 */
class SpringFrameworkVersionTest {

    private static final int[] MINIMUM_PATCHED_VERSION = {7, 0, 7};

    @Test
    void springCoreIsPatched() {
        assertPatched("spring-core", SpringVersion.getVersion());
    }

    @Test
    void springWebIsPatched() {
        assertPatched("spring-web", HttpStatus.class.getPackage().getImplementationVersion());
    }

    private static void assertPatched(String artifact, String version) {
        assertNotNull(version, artifact + " version is not available from the jar manifest");
        assertTrue(compare(version, MINIMUM_PATCHED_VERSION) >= 0,
                artifact + " " + version + " is affected by CVE-2026-22740; expected >= "
                        + Arrays.toString(MINIMUM_PATCHED_VERSION));
    }

    private static int compare(String version, int[] minimum) {
        String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            int part = i < parts.length && parts[i].matches("\\d+") ? Integer.parseInt(parts[i]) : 0;
            if (part != minimum[i]) {
                return Integer.compare(part, minimum[i]);
            }
        }
        return 0;
    }
}
