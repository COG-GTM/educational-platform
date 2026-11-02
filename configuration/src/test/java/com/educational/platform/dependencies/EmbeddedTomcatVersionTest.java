package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing the embedded Tomcat below 11.0.22 (CVE-2026-43515, SNYK-JAVA-ORGAPACHETOMCATEMBED-16690888).
 */
class EmbeddedTomcatVersionTest {

    private static final int[] MINIMUM_VERSION = {11, 0, 22};

    @Test
    void embeddedTomcatIsPatchedForCve202643515() throws Exception {
        String serverNumber = (String) Class.forName("org.apache.catalina.util.ServerInfo")
                .getMethod("getServerNumber")
                .invoke(null);

        assertTrue(isAtLeast(serverNumber, MINIMUM_VERSION),
                () -> "Embedded Tomcat " + serverNumber + " is older than " + Arrays.toString(MINIMUM_VERSION));
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        String[] parts = version.split("\\.");
        for (int i = 0; i < minimum.length; i++) {
            int part = i < parts.length ? Integer.parseInt(parts[i].replaceAll("\\D.*", "")) : 0;
            if (part != minimum[i]) {
                return part > minimum[i];
            }
        }
        return true;
    }
}
