package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.web.servlet.DispatcherServlet;

import java.lang.module.ModuleDescriptor.Version;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2026-41844 (open redirect in spring-webmvc, fixed in 7.0.8).
 */
class SpringFrameworkVersionTest {

    private static final Version MINIMUM = Version.parse("7.0.8");

    @Test
    void springCoreIsPatched() {
        assertPatched("spring-core", SpringVersion.getVersion());
    }

    @Test
    void springWebMvcIsPatched() {
        assertPatched("spring-webmvc", DispatcherServlet.class.getPackage().getImplementationVersion());
    }

    private static void assertPatched(String artifact, String version) {
        assertNotNull(version, artifact + " version is not available on the classpath");
        assertTrue(Version.parse(version).compareTo(MINIMUM) >= 0,
                artifact + " " + version + " is vulnerable to CVE-2026-41844; require >= " + MINIMUM);
    }
}
