package com.educational.platform.version;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.web.servlet.DispatcherServlet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring Framework (spring-webmvc) must be at least 7.0.8 to include the fix for CVE-2026-41842.
 * The Spring Boot 4.0.1 BOM defaults to the vulnerable 7.0.2; the root build pins
 * {@code spring-framework.version}.
 */
public class SpringFrameworkVersionTest {

    @Test
    void springCore_isAtLeast_7_0_8() {
        assertAtLeast_7_0_8("spring-core", SpringVersion.getVersion());
    }

    @Test
    void springWebMvc_isAtLeast_7_0_8() {
        assertAtLeast_7_0_8("spring-webmvc", DispatcherServlet.class.getPackage().getImplementationVersion());
    }

    private static void assertAtLeast_7_0_8(String artifact, String version) {
        assertNotNull(version, artifact + " version should be resolvable");

        final String[] parts = version.split("[.-]");
        final int major = Integer.parseInt(parts[0]);
        final int minor = Integer.parseInt(parts[1]);
        final int patch = Integer.parseInt(parts[2]);

        assertTrue(major > 7 || (major == 7 && (minor > 0 || patch >= 8)),
                artifact + " " + version + " is older than 7.0.8 and exposed to CVE-2026-41842");
    }
}
