package com.educational.platform.dependencies;

import org.h2.util.JdbcUtils;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
        assumeTrue(expected != null, "expected.h2.version system property not set (run via Gradle)");

        assertEquals(expected, resolvedH2Version());
    }

    @Test
    void compare_exactMinimumSafeVersion_isAccepted() {
        assertEquals(0, compare(parse("2.0.206"), MINIMUM_SAFE_VERSION));
    }

    @Test
    void compare_lastVulnerablePatch_isBelowMinimum() {
        assertTrue(compare(parse("2.0.205"), MINIMUM_SAFE_VERSION) < 0);
    }

    @Test
    void compare_snykFindingVersion_isBelowMinimum() {
        assertTrue(compare(parse("1.4.200"), MINIMUM_SAFE_VERSION) < 0);
    }

    @Test
    void compare_newerPatch_isAboveMinimum() {
        assertTrue(compare(parse("2.4.240"), MINIMUM_SAFE_VERSION) > 0);
    }

    @Test
    void compare_shorterVersionWithHigherMinor_isAboveMinimum() {
        assertTrue(compare(parse("2.1"), MINIMUM_SAFE_VERSION) > 0);
    }

    @Test
    void compare_shorterVersionWithLowerMinor_isBelowMinimum() {
        assertTrue(compare(parse("2.0"), MINIMUM_SAFE_VERSION) < 0);
    }

    @Test
    void jndiLookup_withRemoteLdapUrl_isRejectedBeforeLookup() {
        // when
        final SQLException exception = assertThrows(SQLException.class, () ->
                JdbcUtils.getConnection("javax.naming.InitialContext", "ldap://127.0.0.1:1/Exploit", "sa", ""));
        // then
        assertEquals("08001", exception.getSQLState());
        assertTrue(exception.getMessage().contains("Only java scheme is supported for JNDI lookups"), exception.getMessage());
    }

    private static String resolvedH2Version() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:h2-version-check", "sa", "")) {
            return connection.getMetaData().getDatabaseProductVersion().split(" ")[0];
        }
    }

    static int[] parse(String version) {
        return Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
    }

    static int compare(int[] left, int[] right) {
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
