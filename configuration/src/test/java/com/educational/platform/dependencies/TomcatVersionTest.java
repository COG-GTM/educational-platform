package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails if the resolved tomcat-embed-core falls back below 11.0.23
 * (CVE-2026-53434, overridden via {@code ext["tomcat.version"]} in the root build script).
 */
class TomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 23};

    @Test
    void resolvedTomcatVersion_isAtLeast11_0_23() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = parseVersion(serverNumber);

        // then
        assertTrue(Arrays.compare(version, MINIMUM_SAFE_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is below 11.0.23 (CVE-2026-53434)");
    }

    private static int[] parseVersion(String serverNumber) {
        String[] parts = serverNumber.split("\\.");
        int[] version = new int[MINIMUM_SAFE_VERSION.length];
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
}
