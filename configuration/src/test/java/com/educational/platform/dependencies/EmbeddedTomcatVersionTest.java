package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code tomcat.version} BOM override in the root build script: embedded Tomcat
 * before 11.0.22 is affected by CVE-2026-41284 (unbounded resource allocation).
 */
class EmbeddedTomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 22};

    @Test
    void embeddedTomcat_isAtLeastMinimumSafeVersion() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = parseVersion(serverNumber);

        // then
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "Embedded Tomcat " + serverNumber + " is below 11.0.22 (CVE-2026-41284)");
    }

    private static int[] parseVersion(String serverNumber) {
        String[] parts = serverNumber.split("\\.");
        int[] version = {0, 0, 0};
        for (int i = 0; i < version.length && i < parts.length; i++) {
            version[i] = leadingInt(parts[i]);
        }
        return version;
    }

    private static int leadingInt(String value) {
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(value.substring(0, end));
    }

    private static boolean isAtLeast(int[] actual, int[] minimum) {
        for (int i = 0; i < minimum.length; i++) {
            if (actual[i] != minimum[i]) {
                return actual[i] > minimum[i];
            }
        }
        return true;
    }
}
