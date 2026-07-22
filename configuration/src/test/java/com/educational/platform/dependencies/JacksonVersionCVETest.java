package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Jackson 3.x (tools.jackson) version pin against regressions.
 *
 * jackson-databind and jackson-core below 3.1.5 are affected by
 * CVE-2026-54512, CVE-2026-54513, CVE-2026-59889, CVE-2026-54514,
 * CVE-2026-29062 and related advisories. The version is pinned via the
 * Spring Boot BOM property {@code jackson-bom.version}, which a dependency
 * upgrade could silently override.
 */
public class JacksonVersionCVETest {

    private static final Version MINIMUM_SAFE_VERSION = new Version(3, 1, 5, null, "tools.jackson.core", "jackson-core");

    @Test
    void jacksonCoreVersion_isAtLeast315() {
        // given
        final Version resolved = tools.jackson.core.json.PackageVersion.VERSION;

        // then
        assertTrue(resolved.compareTo(MINIMUM_SAFE_VERSION) >= 0,
                "tools.jackson.core:jackson-core must be >= 3.1.5 to remediate CVE-2026-29062 and related advisories, but was " + resolved);
    }

    @Test
    void jacksonDatabindVersion_isAtLeast315() {
        // given
        final Version resolved = tools.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertTrue(resolved.compareTo(MINIMUM_SAFE_VERSION) >= 0,
                "tools.jackson.core:jackson-databind must be >= 3.1.5 to remediate CVE-2026-54512/54513/59889, but was " + resolved);
    }

    @Test
    void jacksonAnnotations_providesJsonSerializeAs_requiredByDatabind315() {
        // jackson-databind 3.1.5 references com.fasterxml.jackson.annotation.JsonSerializeAs
        // (introduced in jackson-annotations 2.21); with annotations managed to 2.20 the
        // application context fails at startup with NoClassDefFoundError.
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"),
                "com.fasterxml.jackson.core:jackson-annotations must be >= 2.21 (provides JsonSerializeAs required by jackson-databind 3.1.5)");
    }
}
