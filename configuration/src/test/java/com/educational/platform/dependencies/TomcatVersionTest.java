package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Embedded Tomcat below 11.0.23 is affected by CVE-2026-55956 (improper authorization).
 */
class TomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 23};

    @Test
    void embeddedTomcat_isAtLeastPatchedVersion() {
        String serverNumber = ServerInfo.getServerNumber();

        assertTrue(compare(parse(serverNumber), MINIMUM_SAFE_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is vulnerable to CVE-2026-55956; upgrade to >= 11.0.23");
    }

    @Test
    void embeddedTomcat_matchesVersionCatalog() {
        String expected = System.getProperty("expected.tomcat.version");

        assertEquals(expected, ServerInfo.getServerNumber().replaceFirst("^(\\d+\\.\\d+\\.\\d+).*", "$1"));
    }

    private static int[] parse(String version) {
        String[] parts = version.split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < result.length && i < parts.length; i++) {
            String digits = parts[i].replaceFirst("^(\\d*).*", "$1");
            result[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return result;
    }

    private static int compare(int[] actual, int[] minimum) {
        for (int i = 0; i < minimum.length; i++) {
            if (actual[i] != minimum[i]) {
                return Integer.compare(actual[i], minimum[i]);
            }
        }
        return 0;
    }
}
