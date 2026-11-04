package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.SpringVersion;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2024-38820 (SNYK-JAVA-ORGSPRINGFRAMEWORK-8298384): spring-core must be at least
 * 6.0.24 on the 6.0 line, 6.1.14 on the 6.1 line, or 6.2+.
 */
class SpringCoreVersionTest {

    private static final int[][] MINIMUM_SAFE_VERSIONS = {{6, 0, 24}, {6, 1, 14}, {6, 2, 0}};

    @Test
    void springCoreIsNotVulnerableToCve202438820() {
        String version = SpringVersion.getVersion();
        assertNotNull(version, "spring-core version could not be determined");
        assertTrue(isSafe(parse(version)),
                () -> "spring-core " + version + " is vulnerable to CVE-2024-38820; upgrade to 6.0.24, 6.1.14 or later");
    }

    @ParameterizedTest
    @CsvSource({
            "7.0.2, true",
            "6.2.0, true",
            "6.3.0, true",
            "6.1.14, true",
            "6.1.15, true",
            "6.0.24, true",
            "6.0.25, true",
            "7.0.2-SNAPSHOT, true",
            "6.2.0-M1, true",
            "6.0.9, false",
            "6.0.23, false",
            "6.1.0, false",
            "6.1.13, false",
            "5.3.31, false",
            "4.3.0, false",
            "7.0, true",
            "7, true"
    })
    void classifiesVersionSafety(String version, boolean expected) {
        // given
        int[] parsedVersion = parse(version);

        // when
        boolean safe = isSafe(parsedVersion);

        // then
        assertEquals(expected, safe);
    }

    @ParameterizedTest
    @CsvSource({
            "7.0.2-SNAPSHOT, 7, 0, 2",
            "6.1.14.RELEASE, 6, 1, 14",
            "6.2.0-M1, 6, 2, 0"
    })
    void parsesQualifiedVersion(String version, int major, int minor, int patch) {
        // given
        int[] expected = {major, minor, patch};

        // when
        int[] parsedVersion = parse(version);

        // then
        assertArrayEquals(expected, parsedVersion);
    }

    @Test
    void singlePartVersionCannotBeCheckedForSafety() {
        // given
        String version = "6";

        // when
        int[] parsedVersion = parse(version);

        // then
        assertArrayEquals(new int[]{6}, parsedVersion);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> isSafe(parsedVersion));
    }

    static boolean isSafe(int[] version) {
        for (int i = MINIMUM_SAFE_VERSIONS.length - 1; i >= 0; i--) {
            int[] minimum = MINIMUM_SAFE_VERSIONS[i];
            boolean sameLine = version[0] == minimum[0] && version[1] == minimum[1];
            if (sameLine || compare(version, minimum) > 0) {
                return compare(version, minimum) >= 0;
            }
        }
        return false;
    }

    static int[] parse(String version) {
        return Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(part -> Integer.parseInt(part.replaceAll("\\D.*", "")))
                .toArray();
    }

    static int compare(int[] left, int[] right) {
        for (int i = 0; i < right.length; i++) {
            int l = i < left.length ? left[i] : 0;
            if (l != right[i]) {
                return Integer.compare(l, right[i]);
            }
        }
        return 0;
    }
}
