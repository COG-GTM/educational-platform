package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Embedded Tomcat below 11.0.20 is vulnerable to HTTP request smuggling (CVE-2026-24880).
 * The version is pinned via {@code tomcat} in {@code gradle/libs.versions.toml}.
 */
class TomcatVersionTest {

    private static final int[] MINIMUM_PATCHED_VERSION = {11, 0, 20};

    @Test
    void embeddedTomcat_isPatchedAgainstRequestSmuggling() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = parse(serverNumber);

        // then
        assertTrue(Arrays.compare(version, MINIMUM_PATCHED_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is below 11.0.20 (CVE-2026-24880)");
    }

    private static int[] parse(String serverNumber) {
        String[] parts = serverNumber.split("\\.");
        int[] version = new int[MINIMUM_PATCHED_VERSION.length];
        for (int i = 0; i < version.length && i < parts.length; i++) {
            String digits = parts[i].replaceAll("\\D.*$", "");
            version[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return version;
    }
}
