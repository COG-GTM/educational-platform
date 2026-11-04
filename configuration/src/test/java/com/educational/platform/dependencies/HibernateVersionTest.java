package com.educational.platform.dependencies;

import org.hibernate.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2020-25638 / SNYK-JAVA-ORGHIBERNATEORM-5926269 (SQL injection), fixed in hibernate-core 6.2.7.Final.
 */
class HibernateVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {6, 2, 7};

    @Test
    void hibernateCoreIsPatchedForCve202025638() {
        // given
        String hibernateVersion = Version.getVersionString();

        // when
        int[] version = parseVersion(hibernateVersion);

        // then
        assertTrue(isAtLeast(version, MINIMUM_SAFE_VERSION),
                "hibernate-core " + hibernateVersion + " is vulnerable to CVE-2020-25638; require >= 6.2.7.Final");
    }

    private static int[] parseVersion(String versionString) {
        String[] parts = versionString.split("\\.");
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
