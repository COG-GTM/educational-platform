package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Jackson 3 version override in the root build: jackson-core below 3.1.1 is affected by
 * SNYK-JAVA-TOOLSJACKSONCORE-15907550 (allocation of resources without limits, CWE-770).
 */
public class JacksonVersionTest {

    @Test
    void jacksonCore_isAtLeast311() {
        final Version resolved = tools.jackson.core.json.PackageVersion.VERSION;

        assertTrue(isAtLeast(resolved, 3, 1, 1),
                "tools.jackson.core:jackson-core must be >= 3.1.1 (SNYK-JAVA-TOOLSJACKSONCORE-15907550), but was " + resolved);
    }

    @Test
    void jacksonDatabind_matchesJacksonCoreMinor() {
        final Version core = tools.jackson.core.json.PackageVersion.VERSION;
        final Version databind = tools.jackson.databind.cfg.PackageVersion.VERSION;

        assertTrue(core.getMajorVersion() == databind.getMajorVersion() && core.getMinorVersion() == databind.getMinorVersion(),
                "jackson-databind " + databind + " must be aligned with jackson-core " + core);
    }

    @Test
    void jacksonAnnotations_providesTypesRequiredByJackson31() {
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"),
                "com.fasterxml.jackson.core:jackson-annotations must be >= 2.21 for Jackson 3.1");
    }

    private static boolean isAtLeast(Version version, int major, int minor, int patch) {
        if (version.getMajorVersion() != major) {
            return version.getMajorVersion() > major;
        }
        if (version.getMinorVersion() != minor) {
            return version.getMinorVersion() > minor;
        }
        return version.getPatchLevel() >= patch;
    }
}
