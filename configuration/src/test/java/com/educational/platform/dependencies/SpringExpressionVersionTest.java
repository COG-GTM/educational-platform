package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2023-20863 (SNYK-JAVA-ORGSPRINGFRAMEWORK-6261585): spring-expression must be 6.0.19 or later.
 */
class SpringExpressionVersionTest {

    private static final int[] MINIMUM_VERSION = {6, 0, 19};

    @Test
    void springExpressionIsNotVulnerableToCve202320863() {
        String version = ExpressionParser.class.getPackage().getImplementationVersion();
        assertNotNull(version, "spring-expression version is not available from the jar manifest");
        int[] actual = Arrays.stream(version.split("[.-]", 4)).limit(3).mapToInt(Integer::parseInt).toArray();
        assertTrue(Arrays.compare(actual, MINIMUM_VERSION) >= 0,
                "spring-expression " + version + " is vulnerable to CVE-2023-20863; upgrade to 6.0.19 or later");
    }
}
