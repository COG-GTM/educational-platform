package com.educational.platform.dependency;

import org.junit.jupiter.api.Test;
import tools.jackson.core.Version;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacksonVersionTest {

    private static final Version MIN_PATCHED_DATABIND =
            new Version(3, 1, 4, null, "tools.jackson.core", "jackson-databind");

    @Test
    void jacksonDatabindIsPatchedForCve202654512() {
        Version version = JsonMapper.builder().build().version();

        assertEquals("tools.jackson.core", version.getGroupId());
        assertEquals("jackson-databind", version.getArtifactId());
        assertTrue(version.compareTo(MIN_PATCHED_DATABIND) >= 0,
                "jackson-databind " + version + " must be >= " + MIN_PATCHED_DATABIND);
    }
}
