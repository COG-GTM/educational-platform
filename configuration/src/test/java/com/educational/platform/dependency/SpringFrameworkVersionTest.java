package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against regressing to a Spring Framework release affected by CVE-2026-41841.
 */
class SpringFrameworkVersionTest {

    private static final int[] MINIMUM_PATCHED = {7, 0, 8};

    @Test
    void springCore_isPatched() {
        assertPatched("spring-core", SpringVersion.getVersion());
    }

    @Test
    void springWebMvc_isPatched() {
        assertPatched("spring-webmvc", DispatcherServlet.class.getPackage().getImplementationVersion());
    }

    private static void assertPatched(String artifact, String version) {
        assertNotNull(version, artifact + " version is not available on the classpath");
        int[] actual = Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();
        assertTrue(Arrays.compare(actual, MINIMUM_PATCHED) >= 0,
                artifact + " " + version + " is vulnerable to CVE-2026-41841; expected >= 7.0.8");
    }
}
