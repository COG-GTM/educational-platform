package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-bom.version} override: jackson-databind below 3.1.5 is affected by
 * CVE-2026-59889 (GHSA-5gvw-p9qm-jgwh).
 */
public class JacksonVersionTest {

    @Test
    void jacksonDatabind_isAtLeast_3_1_5() {
        final Version resolved = tools.jackson.databind.cfg.PackageVersion.VERSION;

        assertTrue(resolved.compareTo(new Version(3, 1, 5, null, resolved.getGroupId(), resolved.getArtifactId())) >= 0,
                "tools.jackson.core:jackson-databind must be >= 3.1.5 (CVE-2026-59889), but was " + resolved);
    }

    @Test
    void jacksonCore_matchesDatabindLine() {
        final Version core = tools.jackson.core.json.PackageVersion.VERSION;

        assertTrue(core.compareTo(new Version(3, 1, 5, null, core.getGroupId(), core.getArtifactId())) >= 0,
                "tools.jackson.core:jackson-core must be >= 3.1.5, but was " + core);
    }

    @Test
    void jacksonAnnotations_providesTypesRequiredByDatabind() {
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"),
                "com.fasterxml.jackson.core:jackson-annotations must be >= 2.21 for jackson-databind 3.1.x");
    }
}
