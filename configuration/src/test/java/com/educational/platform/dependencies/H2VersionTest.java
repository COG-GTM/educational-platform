package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2021-42392 (JNDI RCE in H2 &lt; 2.0.206).
 */
public class H2VersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {2, 0, 206};

    @Test
    void h2OnRuntimeClasspath_isNotVulnerableToCve202142392() throws Exception {
        final String version = resolvedH2Version();

        assertTrue(compare(parse(version), MINIMUM_SAFE_VERSION) >= 0,
                "H2 " + version + " is vulnerable to CVE-2021-42392; upgrade to 2.0.206 or later");
    }

    @Test
    void h2OnRuntimeClasspath_matchesVersionCatalog() throws Exception {
        final String expected = System.getProperty("expected.h2.version");
        if (expected == null) {
            return;
        }

        assertEquals(expected, resolvedH2Version());
    }

    private static String resolvedH2Version() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:h2-version-check", "sa", "")) {
            return connection.getMetaData().getDatabaseProductVersion().split(" ")[0];
        }
    }

    private static int[] parse(String version) {
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    private static int compare(int[] left, int[] right) {
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            final int l = i < left.length ? left[i] : 0;
            final int r = i < right.length ? right[i] : 0;
            if (l != r) {
                return Integer.compare(l, r);
            }
        }
        return 0;
    }
}
