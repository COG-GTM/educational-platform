package com.educational.platform.dependencies;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against SNYK-JAVA-ORGAPACHECOMMONS-6254296 (uncontrolled recursion in
 * {@code ClassUtils.getClass}, fixed in commons-lang3 3.14.0).
 */
class CommonsLang3VersionTest {

    private static final String MINIMUM_SAFE_VERSION = "3.14.0";

    @Test
    void resolvedCommonsLang3IsNotVulnerable() {
        String resolved = resolvedVersion();

        assertTrue(compare(resolved, MINIMUM_SAFE_VERSION) >= 0,
                () -> "commons-lang3 " + resolved + " is below " + MINIMUM_SAFE_VERSION);
    }

    @Test
    void resolvedCommonsLang3MatchesVersionCatalog() {
        String expected = System.getProperty("expected.commons-lang3.version");
        assertNotNull(expected, "expected.commons-lang3.version system property is not set");

        assertEquals(expected, resolvedVersion());
    }

    private static String resolvedVersion() {
        String version = StringUtils.class.getPackage().getImplementationVersion();
        assertNotNull(version, "commons-lang3 jar has no Implementation-Version");
        return version;
    }

    private static int compare(String left, String right) {
        int[] l = numericParts(left);
        int[] r = numericParts(right);
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            int diff = Integer.compare(i < l.length ? l[i] : 0, i < r.length ? r[i] : 0);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    private static int[] numericParts(String version) {
        return Arrays.stream(version.split("[.-]"))
                .takeWhile(part -> part.chars().allMatch(Character::isDigit) && !part.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
