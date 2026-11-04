package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against SNYK-JAVA-ORGSLF4J-5942503 (CWE-117), fixed in slf4j-api 2.0.9.
 */
class Slf4jVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {2, 0, 9};

    @Test
    void slf4jApiOnClasspathIsPatched() {
        String actual = slf4jApiVersion();
        assertTrue(Arrays.compare(parse(actual), MINIMUM_SAFE_VERSION) >= 0,
                "slf4j-api " + actual + " is vulnerable to SNYK-JAVA-ORGSLF4J-5942503; require >= 2.0.9");
    }

    @Test
    void slf4jApiOnClasspathMatchesVersionCatalog() {
        String expected = System.getProperty("expected.slf4j.version");
        assertNotNull(expected, "expected.slf4j.version system property is not set");
        assertEquals(expected, slf4jApiVersion());
    }

    private static String slf4jApiVersion() {
        String version = LoggerFactory.class.getPackage().getImplementationVersion();
        assertNotNull(version, "slf4j-api jar has no Implementation-Version");
        return version;
    }

    private static int[] parse(String version) {
        String[] parts = version.split("[.-]");
        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        };
    }
}
