package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedTomcatVersionTest {

    // CVE-2026-25854 (open redirect) is fixed in Tomcat 11.0.20.
    private static final int[] MINIMUM_TOMCAT_VERSION = {11, 0, 20};

    @Test
    void embeddedTomcatIsNotVulnerableToCve202625854() {
        String serverNumber = ServerInfo.getServerNumber();
        int[] actual = Arrays.stream(serverNumber.split("[^0-9]+"))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();

        assertTrue(Arrays.compare(actual, MINIMUM_TOMCAT_VERSION) >= 0,
                "Embedded Tomcat " + serverNumber + " is older than 11.0.20 (CVE-2026-25854)");
    }
}
