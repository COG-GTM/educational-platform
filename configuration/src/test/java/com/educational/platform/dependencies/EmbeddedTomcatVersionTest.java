package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.apache.tomcat.websocket.WsSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails if the embedded Tomcat on the runtime classpath falls back below 11.0.22
 * (CVE-2026-42498 in tomcat-embed-websocket), e.g. when the {@code tomcat} version
 * override in {@code gradle/libs.versions.toml} is removed.
 */
class EmbeddedTomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 22};

    @Test
    void tomcatEmbedCore_isAtLeastMinimumSafeVersion() {
        assertAtLeastMinimum("tomcat-embed-core", ServerInfo.getServerNumber());
    }

    @Test
    void tomcatEmbedWebsocket_isAtLeastMinimumSafeVersion() {
        String version = WsSession.class.getPackage().getImplementationVersion();

        assertNotNull(version, "tomcat-embed-websocket jar has no Implementation-Version");
        assertAtLeastMinimum("tomcat-embed-websocket", version);
    }

    private static void assertAtLeastMinimum(String artifact, String version) {
        assertTrue(isAtLeast(parseVersion(version), MINIMUM_SAFE_VERSION),
                artifact + " " + version + " is below 11.0.22 (CVE-2026-42498)");
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] parsed = new int[MINIMUM_SAFE_VERSION.length];
        for (int i = 0; i < parsed.length && i < parts.length; i++) {
            String digits = parts[i].replaceFirst("\\D.*$", "");
            parsed[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return parsed;
    }

    private static boolean isAtLeast(int[] actual, int[] minimum) {
        for (int i = 0; i < minimum.length; i++) {
            if (actual[i] != minimum[i]) {
                return actual[i] > minimum[i];
            }
        }
        return true;
    }
}
