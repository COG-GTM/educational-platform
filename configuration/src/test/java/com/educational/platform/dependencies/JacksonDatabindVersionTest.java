package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the {@code jackson-2-bom.version} override: jackson-databind below 2.21.4 is affected by CVE-2026-54513.
 */
class JacksonDatabindVersionTest {

    private static final Version MINIMUM = new Version(2, 21, 4, null, "com.fasterxml.jackson.core", "jackson-databind");

    private static final Version DATABIND = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

    @Test
    void jacksonDatabindIsPatchedForCve202654513() {
        final Version resolved = DATABIND;

        assertTrue(resolved.compareTo(MINIMUM) >= 0,
                () -> "jackson-databind " + resolved + " is below " + MINIMUM + " (CVE-2026-54513)");
    }

    @Test
    void jacksonCoreIsAlignedWithDatabind() {
        // given
        final Version core = com.fasterxml.jackson.core.json.PackageVersion.VERSION;

        // when / then
        assertEquals(releaseOf(DATABIND), releaseOf(core),
                () -> "jackson-core " + core + " is not aligned with jackson-databind " + DATABIND);
    }

    @Test
    void registeredJackson2ModulesAreAlignedWithDatabind() {
        // when
        final List<Module> modules = ObjectMapper.findModules();

        // then
        assertFalse(modules.isEmpty(), "expected Jackson 2 modules (e.g. jackson-datatype-jsr310) on the classpath");
        for (Module module : modules) {
            assertEquals(releaseOf(DATABIND), releaseOf(module.version()),
                    () -> module.getModuleName() + " " + module.version() + " is not aligned with jackson-databind " + DATABIND);
        }
    }

    private static String releaseOf(Version version) {
        return version.getMajorVersion() + "." + version.getMinorVersion() + "." + version.getPatchLevel();
    }
}
