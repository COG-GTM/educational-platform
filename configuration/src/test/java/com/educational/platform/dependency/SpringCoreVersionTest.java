package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2024-38820 (SNYK-JAVA-ORGSPRINGFRAMEWORK-8298384): spring-core must be at least
 * 6.0.24 on the 6.0 line, 6.1.14 on the 6.1 line, or 6.2+.
 */
class SpringCoreVersionTest {

    private static final int[][] MINIMUM_SAFE_VERSIONS = {{6, 0, 24}, {6, 1, 14}, {6, 2, 0}};

    @Test
    void springCoreIsNotVulnerableToCve202438820() {
        String version = SpringVersion.getVersion();
        assertNotNull(version, "spring-core version could not be determined");
        assertTrue(isSafe(parse(version)),
                () -> "spring-core " + version + " is vulnerable to CVE-2024-38820; upgrade to 6.0.24, 6.1.14 or later");
    }

    private static boolean isSafe(int[] version) {
        for (int i = MINIMUM_SAFE_VERSIONS.length - 1; i >= 0; i--) {
            int[] minimum = MINIMUM_SAFE_VERSIONS[i];
            boolean sameLine = version[0] == minimum[0] && version[1] == minimum[1];
            if (sameLine || compare(version, minimum) > 0) {
                return compare(version, minimum) >= 0;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        return Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(part -> Integer.parseInt(part.replaceAll("\\D.*", "")))
                .toArray();
    }

    private static int compare(int[] left, int[] right) {
        for (int i = 0; i < right.length; i++) {
            int l = i < left.length ? left[i] : 0;
            if (l != right[i]) {
                return Integer.compare(l, right[i]);
            }
        }
        return 0;
    }
}
