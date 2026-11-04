package com.educational.platform.dependencies;

import org.apache.tomcat.websocket.WsSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against tomcat-embed-websocket falling back to a version affected by
 * CVE-2024-23672 (WebSocket DoS, fixed in 10.1.20), e.g. when the {@code tomcat}
 * override in {@code gradle/libs.versions.toml} is removed or downgraded.
 */
class TomcatWebSocketVersionTest {

    private static final String CVE_2024_23672_FIXED_VERSION = "10.1.20";

    @Test
    void tomcatEmbedWebsocket_isNotAffectedByCve202423672() {
        String version = webSocketVersion();

        assertTrue(compare(version, CVE_2024_23672_FIXED_VERSION) >= 0,
                "tomcat-embed-websocket " + version + " is below " + CVE_2024_23672_FIXED_VERSION
                        + " (CVE-2024-23672)");
    }

    @Test
    void tomcatEmbedWebsocket_matchesVersionCatalogPin() {
        String expected = System.getProperty("expected.tomcat.version");

        assertNotNull(expected, "expected.tomcat.version system property is not set by Gradle");
        assertEquals(expected, webSocketVersion());
    }

    private static String webSocketVersion() {
        String version = WsSession.class.getPackage().getImplementationVersion();
        assertNotNull(version, "tomcat-embed-websocket jar has no Implementation-Version");
        return version;
    }

    static int compare(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int diff = Integer.compare(numericPart(a, i), numericPart(b, i));
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int numericPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        String digits = parts[index].replaceFirst("\\D.*$", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
