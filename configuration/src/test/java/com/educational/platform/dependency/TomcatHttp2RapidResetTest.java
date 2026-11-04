package com.educational.platform.dependency;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2023-44487 (HTTP/2 Rapid Reset) is fixed in Tomcat 8.5.94, 9.0.81, 10.1.14 and every 11.0.x GA release.
 */
class TomcatHttp2RapidResetTest {

    private static final Map<String, int[]> FIRST_PATCHED_PER_LINE = Map.of(
            "8.5", new int[]{8, 5, 94},
            "9.0", new int[]{9, 0, 81},
            "10.1", new int[]{10, 1, 14},
            "11.0", new int[]{11, 0, 0});

    @Test
    void embeddedTomcat_isPatchedForCve202344487() {
        String serverNumber = ServerInfo.getServerNumber();
        int[] actual = Arrays.stream(serverNumber.split("\\.", 4))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();

        int[] firstPatched = FIRST_PATCHED_PER_LINE.get(actual[0] + "." + actual[1]);
        boolean patched = firstPatched == null
                ? actual[0] > 11
                : Arrays.compare(actual, firstPatched) >= 0;

        assertTrue(patched, "tomcat-embed-core " + serverNumber
                + " is vulnerable to CVE-2023-44487 (HTTP/2 Rapid Reset); 10.1.14 / 11.0.x or later is required");
    }
}
