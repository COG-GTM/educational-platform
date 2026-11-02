package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Jackson 3 version override in the root build script.
 * jackson-core below 3.1.0 is affected by CVE-2026-29062 (SNYK-JAVA-TOOLSJACKSONCORE-15371178).
 */
public class JacksonVersionTest {

    @Test
    void jacksonCore_isNotVulnerableToCve202629062() {
        final Version resolved = tools.jackson.core.json.PackageVersion.VERSION;

        assertTrue(isAtLeast(resolved, 3, 1, 0),
                "tools.jackson.core:jackson-core must be >= 3.1.0 (CVE-2026-29062), but was " + resolved);
    }

    @Test
    void jacksonDatabind_matchesJacksonCore() {
        final Version core = tools.jackson.core.json.PackageVersion.VERSION;
        final Version databind = tools.jackson.databind.cfg.PackageVersion.VERSION;

        assertEquals(core.getMajorVersion(), databind.getMajorVersion());
        assertEquals(core.getMinorVersion(), databind.getMinorVersion());
    }

    @Test
    void jacksonAnnotations_isCompatibleWithDatabind() {
        // jackson-databind 3.1 requires jackson-annotations 2.21, which introduced JsonSerializeAs.
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonSerializeAs"));
    }

    @Test
    void jsonMapper_roundTrips() {
        final JsonMapper mapper = JsonMapper.builder().build();

        final String json = mapper.writeValueAsString(Map.of("name", "course", "rating", 5));
        final Map<?, ?> read = mapper.readValue(json, Map.class);

        assertEquals("course", read.get("name"));
        assertEquals(5, read.get("rating"));
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
