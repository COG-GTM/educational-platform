package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the tools.jackson version pinned via the Spring Boot BOM property {@code jackson-bom.version}.
 * jackson-databind below 3.1.4 is affected by CVE-2026-59888 (SNYK-JAVA-TOOLSJACKSONCORE-17972436, GHSA-3pjw-73gf-8qr5).
 */
public class JacksonVersionTest {

    @Test
    void jacksonDatabindVersion_isAtLeast314() {
        final Version resolved = tools.jackson.databind.cfg.PackageVersion.VERSION;

        assertTrue(isAtLeast(resolved, 3, 1, 4),
                "tools.jackson.core:jackson-databind must be >= 3.1.4 (CVE-2026-59888), but was " + resolved);
    }

    @Test
    void jacksonCoreVersion_isAtLeast314() {
        final Version resolved = tools.jackson.core.json.PackageVersion.VERSION;

        assertTrue(isAtLeast(resolved, 3, 1, 4),
                "tools.jackson.core:jackson-core must be >= 3.1.4 to match jackson-databind, but was " + resolved);
    }

    @Test
    void jacksonAnnotations_providesTypesRequiredByDatabind31() {
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"),
                "com.fasterxml.jackson.core:jackson-annotations must be >= 2.21 for jackson-databind 3.1.x");
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
