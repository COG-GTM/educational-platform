package com.educational.platform.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;
import org.springframework.web.servlet.DispatcherServlet;

class SpringFrameworkVersionTest {

    // CVE-2026-41846 (spring-webmvc XSS) is fixed in 7.0.8.
    private static final int[] MINIMUM_PATCHED_VERSION = {7, 0, 8};

    @Test
    void springCoreIsPatched() {
        assertPatched(SpringVersion.getVersion());
    }

    @Test
    void springWebMvcIsPatched() {
        assertPatched(DispatcherServlet.class.getPackage().getImplementationVersion());
    }

    private static void assertPatched(String version) {
        assertThat(version).as("Spring Framework version").isNotBlank();
        int[] actual = Arrays.stream(version.split("[.-]"))
                .limit(3)
                .mapToInt(Integer::parseInt)
                .toArray();
        assertThat(Arrays.compare(actual, MINIMUM_PATCHED_VERSION))
                .as("Spring Framework %s must be >= 7.0.8", version)
                .isGreaterThanOrEqualTo(0);
    }
}
