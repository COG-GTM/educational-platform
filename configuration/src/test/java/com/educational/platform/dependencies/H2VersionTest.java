package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing to an H2 release affected by CVE-2018-10054 (fixed in 2.0.206).
 */
class H2VersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {2, 0, 206};

    @Test
    void runtimeH2VersionIsNotVulnerableToCve201810054() throws SQLException {
        String productVersion;
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:h2-version-check", "sa", "")) {
            productVersion = connection.getMetaData().getDatabaseProductVersion();
        }

        assertTrue(isAtLeast(productVersion, MINIMUM_SAFE_VERSION),
                () -> "H2 " + productVersion + " is affected by CVE-2018-10054; upgrade to 2.0.206 or later");
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        String[] parts = version.split("[^0-9]+");
        for (int i = 0; i < minimum.length; i++) {
            int actual = i < parts.length && !parts[i].isEmpty() ? Integer.parseInt(parts[i]) : 0;
            if (actual != minimum[i]) {
                return actual > minimum[i];
            }
        }
        return true;
    }
}
