package com.educational.platform.dependency;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CVE-2023-44487 (HTTP/2 Rapid Reset) is fixed in Tomcat 8.5.94, 9.0.81, 10.1.14 and every 11.0.x GA release.
 */
class TomcatHttp2RapidResetTest {

    private static final Map<String, int[]> FIRST_PATCHED_PER_LINE = Map.of(
            "8.5", new int[]{8, 5, 94},
            "9.0", new int[]{9, 0, 81},
            "10.1", new int[]{10, 1, 14},
            "11.0", new int[]{11, 0, 0});

    @Test
    void embeddedTomcat_isPatchedForCve202344487() {
        // given
        String serverNumber = ServerInfo.getServerNumber();

        // when
        boolean patched = isPatched(serverNumber);

        // then
        assertTrue(patched, "tomcat-embed-core " + serverNumber
                + " is vulnerable to CVE-2023-44487 (HTTP/2 Rapid Reset); 10.1.14 / 11.0.x or later is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.5.94", "9.0.81", "10.1.14", "11.0.0", "11.0.15",
            "10.1.14.0", "11.0.15.0", "12.0.0"})
    void patchedVersionsPass(String serverNumber) {
        // given / when
        boolean patched = isPatched(serverNumber);

        // then
        assertTrue(patched);
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.5.93", "9.0.80", "10.1.13", "10.1.8", "10.0.27", "9.1.0", "7.0.109"})
    void vulnerableOrUnknownOlderVersionsFail(String serverNumber) {
        // given / when
        boolean patched = isPatched(serverNumber);

        // then
        assertFalse(patched);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", ""})
    void nonNumericVersionsThrow(String serverNumber) {
        // given / when / then
        assertThrows(NumberFormatException.class, () -> isPatched(serverNumber));
    }

    @Test
    void missingMinorVersionThrows() {
        // given
        String serverNumber = "11";

        // when / then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> isPatched(serverNumber));
    }

    static boolean isPatched(String serverNumber) {
        int[] actual = Arrays.stream(serverNumber.split("\\.", 4))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();

        int[] firstPatched = FIRST_PATCHED_PER_LINE.get(actual[0] + "." + actual[1]);
        return firstPatched == null
                ? actual[0] > 11
                : Arrays.compare(actual, firstPatched) >= 0;
    }
}
