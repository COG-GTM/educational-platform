package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2026-41850 (SNYK-JAVA-ORGSPRINGFRAMEWORK-17253311) is fixed in spring-expression 7.0.8.
 */
class SpringExpressionVersionTest {

    private static final int[] MINIMUM_VERSION = {7, 0, 8};

    @Test
    void springExpression_isAtLeastMinimumPatchedVersion() {
        assertAtLeastMinimum(SpelExpressionParser.class.getPackage().getImplementationVersion());
    }

    @Test
    void springCore_matchesPatchedFrameworkVersion() {
        assertAtLeastMinimum(SpringVersion.getVersion());
    }

    @Test
    void spelExpressions_stillEvaluate() {
        assertEquals(42, new SpelExpressionParser().parseExpression("6 * 7").getValue(Integer.class));
    }

    private static void assertAtLeastMinimum(String version) {
        assertNotNull(version, "Spring Framework version must be resolvable");
        int[] actual = Arrays.stream(version.split("[.-]")).limit(3).mapToInt(Integer::parseInt).toArray();
        assertEquals(3, actual.length, "Unexpected Spring Framework version format: " + version);
        assertTrue(Arrays.compare(actual, MINIMUM_VERSION) >= 0,
                "spring-expression must be >= 7.0.8 (CVE-2026-41850), but was " + version);
    }
}
