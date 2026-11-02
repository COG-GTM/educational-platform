package com.educational.platform.dependency;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedTomcatVersionTest {

    private static final int[] MINIMUM_PATCHED_VERSION = {11, 0, 23};

    @Test
    void embeddedTomcat_isPatchedForCve202655955() {
        String serverNumber = ServerInfo.getServerNumber();
        int[] actual = Arrays.stream(serverNumber.split("\\.", 4))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();

        assertTrue(Arrays.compare(actual, MINIMUM_PATCHED_VERSION) >= 0,
                "tomcat-embed-core " + serverNumber + " is vulnerable to CVE-2026-55955; 11.0.23 or later is required");
    }
}
