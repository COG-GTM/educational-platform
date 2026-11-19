package com.educational.platform.dependencies;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code tomcat.version} BOM override in the root build script: embedded Tomcat
 * before 11.0.22 is affected by CVE-2026-41284 (unbounded resource allocation).
 * It also guards that tomcat-embed-el and tomcat-embed-websocket remain aligned with core.
 */
class EmbeddedTomcatVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {11, 0, 22};

    @Test
    void embeddedTomcat_isAtLeastMinimumSafeVersion() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        int[] version = parseVersion(serverNumber);

        // then
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "Embedded Tomcat " + serverNumber + " is below 11.0.22 (CVE-2026-41284)");
    }

    @Test
    void embeddedTomcatModules_allResolveToSameVersionAsCore() throws ClassNotFoundException {
        // given
        String coreVersion = implementationVersion("org.apache.catalina.util.ServerInfo");

        // when
        String elVersion = implementationVersion("org.apache.el.ExpressionFactoryImpl");
        String websocketVersion = implementationVersion("org.apache.tomcat.websocket.WsWebSocketContainer");

        // then
        assertAll(
                () -> assertTrue(isAtLeast(parseVersion(coreVersion), MINIMUM_SAFE_VERSION),
                        "tomcat-embed-core " + coreVersion + " is below 11.0.22 (CVE-2026-41284)"),
                () -> assertEquals(coreVersion, elVersion, "tomcat-embed-el must match tomcat-embed-core"),
                () -> assertEquals(coreVersion, websocketVersion, "tomcat-embed-websocket must match tomcat-embed-core"));
    }

    private static String implementationVersion(String className) throws ClassNotFoundException {
        String version = Class.forName(className).getPackage().getImplementationVersion();
        assertNotNull(version, "No Implementation-Version in manifest of jar containing " + className);
        return version;
    }

    private static int[] parseVersion(String serverNumber) {
        String[] parts = serverNumber.split("\\.");
        int[] version = {0, 0, 0};
        for (int i = 0; i < version.length && i < parts.length; i++) {
            version[i] = leadingInt(parts[i]);
        }
        return version;
    }

    private static int leadingInt(String value) {
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        return end == 0 ? 0 : Integer.parseInt(value.substring(0, end));
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
