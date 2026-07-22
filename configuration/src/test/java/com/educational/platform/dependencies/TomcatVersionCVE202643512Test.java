package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the embedded Tomcat version override against regressions.
 *
 * <p>Tomcat versions below 11.0.23 are affected by multiple CVEs, including
 * CVE-2026-43512 (Critical, improper authentication) and CVE-2026-24880
 * (High, request smuggling). The override is applied via
 * {@code ext["tomcat.version"] = "11.0.23"} in the root build script; this test
 * fails if the resolved tomcat-embed-core artifact ever falls back below the
 * patched version.
 */
class TomcatVersionCVE202643512Test {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 23};

    @Test
    void resolvedTomcatVersion_isAtLeast11_0_23() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = parseVersion(serverNumber);

        // then
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "Embedded Tomcat version " + serverNumber
                        + " is below the minimum safe version 11.0.23 "
                        + "(CVE-2026-43512 and 17 other CVEs)");
    }

    /**
     * Parses the leading numeric components of a Tomcat version string into three
     * ints, tolerating fewer than three dotted parts (missing parts default to 0)
     * and non-numeric suffixes such as {@code -M1} or {@code -RC2}.
     */
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
