package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.tomcat.websocket.WsSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
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

    @Test
    void tomcatEmbedCore_matchesWebsocketVersion() {
        assertEquals(webSocketVersion(), ServerInfo.class.getPackage().getImplementationVersion());
    }

    @Test
    void tomcatEmbedEl_matchesWebsocketVersion() {
        assertEquals(webSocketVersion(), ExpressionFactoryImpl.class.getPackage().getImplementationVersion());
    }

    @Test
    void compare_exactFixedVersion_isEqual() {
        assertEquals(0, compare("10.1.20", CVE_2024_23672_FIXED_VERSION));
    }

    @Test
    void compare_vulnerableVersions_areBelowFixedVersion() {
        assertAll(
                () -> assertTrue(compare("10.1.19", CVE_2024_23672_FIXED_VERSION) < 0),
                () -> assertTrue(compare("10.1.8", CVE_2024_23672_FIXED_VERSION) < 0),
                () -> assertTrue(compare("10.1.9", "10.1.10") < 0)
        );
    }

    @Test
    void compare_newerVersions_areAboveFixedVersion() {
        assertAll(
                () -> assertTrue(compare("10.1.21", CVE_2024_23672_FIXED_VERSION) > 0),
                () -> assertTrue(compare("11.0.23", CVE_2024_23672_FIXED_VERSION) > 0),
                () -> assertTrue(compare("10.1.10", "10.1.9") > 0)
        );
    }

    @Test
    void compare_majorAndMinorVersions_takePrecedenceOverPatch() {
        assertAll(
                () -> assertTrue(compare("10.9.99", "11.0.0") < 0),
                () -> assertTrue(compare("11.0.0", "10.9.99") > 0),
                () -> assertTrue(compare("10.0.99", "10.1.0") < 0),
                () -> assertTrue(compare("10.1.0", "10.0.99") > 0)
        );
    }

    @Test
    void compare_missingComponents_areTreatedAsZero() {
        assertAll(
                () -> assertEquals(0, compare("11", "11.0.0")),
                () -> assertEquals(0, compare("11.0.0", "11")),
                () -> assertTrue(compare("11.0", "11.0.1") < 0),
                () -> assertTrue(compare("11.0.1", "11.0") > 0)
        );
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
