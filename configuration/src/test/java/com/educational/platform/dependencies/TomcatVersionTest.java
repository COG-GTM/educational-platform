package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails if the embedded Tomcat resolved on the runtime classpath falls back below 11.0.18
 * (CVE-2026-24734, incorrect authorization), e.g. when the {@code tomcat.version} override
 * in the root build script is removed.
 */
class TomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 18};

    @Test
    void embeddedTomcat_isPatchedForCve202624734() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = parseVersion(serverNumber);

        // then
        assertTrue(Arrays.compare(version, MINIMUM_SAFE_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is below 11.0.18 (CVE-2026-24734)");
    }

    private static int[] parseVersion(String serverNumber) {
        String[] parts = serverNumber.split("\\.");
        int[] version = new int[MINIMUM_SAFE_VERSION.length];
        for (int i = 0; i < version.length && i < parts.length; i++) {
            String digits = parts[i].replaceFirst("\\D.*$", "");
            version[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return version;
    }
}
