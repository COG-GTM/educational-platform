package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards against CVE-2024-22243 (SNYK-JAVA-ORGSPRINGFRAMEWORK-6261586): spring-web before 5.3.32,
 * 6.0.17 and 6.1.4 parses {@code https://trusted[@attacker} with host {@code trusted}, while browsers
 * and HTTP clients connect to {@code attacker}, enabling SSRF and open redirect bypasses.
 */
class SpringWebSsrfCve202422243Test {

    private static final String CONFUSING_URL = "https://trusted.example[@attacker.example/path";

    @Test
    void springWeb_isNotInVulnerableRange() {
        String version = UriComponentsBuilder.class.getPackage().getImplementationVersion();
        assertNotNull(version, "spring-web Implementation-Version is missing from the jar manifest");

        int[] v = parse(version);
        boolean vulnerable = compare(v, 5, 3, 32) < 0
                || (compare(v, 6, 0, 0) >= 0 && compare(v, 6, 0, 17) < 0)
                || (compare(v, 6, 1, 0) >= 0 && compare(v, 6, 1, 4) < 0);

        assertFalse(vulnerable, "spring-web " + version + " is affected by CVE-2024-22243; upgrade to 6.0.17+ / 6.1.4+");
    }

    @Test
    void uriComponentsBuilder_doesNotTreatUserInfoBracketAsHost() {
        UriComponents components;
        try {
            components = UriComponentsBuilder.fromUriString(CONFUSING_URL).build();
        } catch (IllegalArgumentException rejectedAsMalformed) {
            return;
        }
        assertNotEquals("trusted.example", components.getHost(),
                "UriComponentsBuilder reported the userinfo part as host");
    }

    private static int[] parse(String version) {
        return Arrays.stream(version.split("[.-]", 4))
                .limit(3)
                .mapToInt(part -> Integer.parseInt(part.replaceAll("\\D.*", "")))
                .toArray();
    }

    private static int compare(int[] actual, int major, int minor, int patch) {
        int[] expected = {major, minor, patch};
        for (int i = 0; i < expected.length; i++) {
            int part = i < actual.length ? actual[i] : 0;
            if (part != expected[i]) {
                return Integer.compare(part, expected[i]);
            }
        }
        return 0;
    }
}
