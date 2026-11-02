package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tomcat below 11.0.23 is affected by CVE-2026-53404; the BOM override lives in the root build script.
 */
class EmbeddedTomcatVersionTest {

    private static final int[] MINIMUM_PATCHED_VERSION = {11, 0, 23};

    @Test
    void embeddedTomcat_isAtLeastPatchedVersion() {
        String serverNumber = ServerInfo.getServerNumber();

        assertTrue(Arrays.compare(parse(serverNumber), MINIMUM_PATCHED_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is below 11.0.23 (CVE-2026-53404)");
    }

    private static int[] parse(String version) {
        String[] parts = version.split("\\.");
        int[] numbers = new int[MINIMUM_PATCHED_VERSION.length];
        for (int i = 0; i < numbers.length && i < parts.length; i++) {
            String digits = parts[i].replaceAll("\\D.*", "");
            numbers[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return numbers;
    }
}
