package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the tools.jackson version pinned via the Spring Boot BOM property {@code jackson-bom.version}.
 * jackson-databind below 3.1.4 is affected by CVE-2026-54517 (GHSA-5hh8-q8hv-fr38).
 */
public class JacksonVersionTest {

    @Test
    void jacksonDatabindVersion_isAtLeast314() {
        // given
        final Version resolved = tools.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertTrue(isAtLeast(resolved, 3, 1, 4),
                "tools.jackson.core:jackson-databind must be >= 3.1.4 (CVE-2026-54517), but was " + resolved);
    }

    @Test
    void jacksonCoreVersion_matchesDatabindMinorLine() {
        // given
        final Version core = tools.jackson.core.json.PackageVersion.VERSION;
        final Version databind = tools.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertEquals(databind.getMajorVersion(), core.getMajorVersion(), "jackson-core major must match databind");
        assertEquals(databind.getMinorVersion(), core.getMinorVersion(), "jackson-core minor must match databind");
        assertTrue(isAtLeast(core, 3, 1, 4), "tools.jackson.core:jackson-core must be >= 3.1.4, but was " + core);
    }

    @Test
    void jacksonAnnotations_providesTypesRequiredByDatabind31() {
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"),
                "com.fasterxml.jackson.core:jackson-annotations must be >= 2.21 for jackson-databind 3.1.x");
    }

    @Test
    void jsonMapper_roundTripsWithResolvedJacksonArtifacts() {
        // given
        final JsonMapper sut = JsonMapper.builder().build();

        // when
        final String json = sut.writeValueAsString(Map.of("name", "course"));
        final Map<?, ?> parsed = sut.readValue(json, Map.class);

        // then
        assertFalse(json.isBlank());
        assertEquals("course", parsed.get("name"));
    }

    @Test
    void isAtLeast_comparesMajorMinorPatchNumerically() {
        assertTrue(isAtLeast(new Version(3, 1, 4, null, null, null), 3, 1, 4));
        assertTrue(isAtLeast(new Version(3, 1, 10, null, null, null), 3, 1, 4));
        assertTrue(isAtLeast(new Version(3, 2, 0, null, null, null), 3, 1, 4));
        assertTrue(isAtLeast(new Version(4, 0, 0, null, null, null), 3, 1, 4));
        assertFalse(isAtLeast(new Version(3, 1, 3, null, null, null), 3, 1, 4));
        assertFalse(isAtLeast(new Version(3, 0, 9, null, null, null), 3, 1, 4));
        assertFalse(isAtLeast(new Version(2, 21, 4, null, null, null), 3, 1, 4));
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
