package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Embedded Tomcat below 11.0.20 is affected by CVE-2026-29145 (improper authentication).
 * The version is pinned through {@code tomcat} in {@code gradle/libs.versions.toml}.
 */
class TomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 20};

    @Test
    void embeddedTomcat_isNotVulnerableToCve2026_29145() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = Arrays.stream(serverNumber.split("\\.")).limit(3).mapToInt(Integer::parseInt).toArray();

        // then
        assertTrue(Arrays.compare(version, MINIMUM_SAFE_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is affected by CVE-2026-29145; upgrade to 11.0.20 or later");
    }
}
