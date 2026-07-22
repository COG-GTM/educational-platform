package com.educational.platform.dependency;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Logback version pin remediating CVE-2026-13006, CVE-2026-9828 and
 * CVE-2026-1225. All three are fixed in logback 1.5.36, so the resolved runtime
 * versions of logback-classic and logback-core must never fall below it.
 */
class LogbackVersionCVEGuardTest {

    private static final int[] MIN_FIXED_VERSION = {1, 5, 36};

    @Test
    void logbackClassic_resolvedVersion_isAtLeastFixedVersion() {
        // given
        final String version = implementationVersionOf(Logger.class);

        // then
        assertNotNull(version, "logback-classic version could not be determined");
        assertTrue(isAtLeast(version, MIN_FIXED_VERSION),
                "logback-classic version " + version + " is below the CVE fix version 1.5.36");
    }

    @Test
    void logbackCore_resolvedVersion_isAtLeastFixedVersion() {
        // given
        final String version = implementationVersionOf(Appender.class);

        // then
        assertNotNull(version, "logback-core version could not be determined");
        assertTrue(isAtLeast(version, MIN_FIXED_VERSION),
                "logback-core version " + version + " is below the CVE fix version 1.5.36");
    }

    private static String implementationVersionOf(Class<?> clazz) {
        final String packageVersion = clazz.getPackage().getImplementationVersion();
        if (packageVersion != null) {
            return packageVersion;
        }
        try {
            final URL manifestUrl = URI.create("jar:" + clazz.getProtectionDomain().getCodeSource().getLocation() + "!/META-INF/MANIFEST.MF").toURL();
            try (var stream = manifestUrl.openStream()) {
                return new Manifest(stream).getMainAttributes().getValue("Implementation-Version");
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            final int part = i < parts.length ? parseNumeric(parts[i]) : 0;
            if (part > minimum[i]) {
                return true;
            }
            if (part < minimum[i]) {
                return false;
            }
        }
        return true;
    }

    private static int parseNumeric(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
