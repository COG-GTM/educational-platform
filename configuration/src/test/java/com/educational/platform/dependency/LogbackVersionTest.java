package com.educational.platform.dependency;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2023-6481 (SNYK-JAVA-CHQOSLOGBACK-6097492, SSRF/DoS via the receiver component) is fixed in logback 1.4.12.
 */
class LogbackVersionTest {

    private static final int[] MIN_FIXED_VERSION = {1, 4, 12};

    @Test
    void logbackClassic_resolvedVersion_isAtLeastFixedVersion() {
        assertAtLeastFixedVersion("logback-classic", Logger.class);
    }

    @Test
    void logbackCore_resolvedVersion_isAtLeastFixedVersion() {
        assertAtLeastFixedVersion("logback-core", Appender.class);
    }

    @Test
    void logbackClassicAndCore_resolveToSameVersion() {
        final String classicVersion = Logger.class.getPackage().getImplementationVersion();
        final String coreVersion = Appender.class.getPackage().getImplementationVersion();
        assertNotNull(classicVersion, "logback-classic version could not be determined");
        assertEquals(classicVersion, coreVersion, "logback-classic and logback-core must resolve to the same version");
    }

    @Test
    void logbackClassic_manifestVersion_matchesMavenPomVersion() throws IOException {
        assertManifestMatchesPomVersion("logback-classic", Logger.class);
    }

    @Test
    void logbackCore_manifestVersion_matchesMavenPomVersion() throws IOException {
        assertManifestMatchesPomVersion("logback-core", Appender.class);
    }

    @Test
    void slf4j_isBoundToLogback() {
        assertInstanceOf(LoggerContext.class, LoggerFactory.getILoggerFactory(),
                "SLF4J must bind to logback, otherwise the guarded logback version is not the one in use");
    }

    private static void assertManifestMatchesPomVersion(String artifact, Class<?> clazz) throws IOException {
        final String resource = "/META-INF/maven/ch.qos.logback/" + artifact + "/pom.properties";
        final Properties pom = new Properties();
        try (InputStream in = clazz.getResourceAsStream(resource)) {
            assertNotNull(in, artifact + " pom.properties not found at " + resource);
            pom.load(in);
        }
        assertEquals(pom.getProperty("version"), clazz.getPackage().getImplementationVersion(),
                artifact + " manifest Implementation-Version must match the Maven artifact version");
    }

    private static void assertAtLeastFixedVersion(String artifact, Class<?> clazz) {
        final String version = clazz.getPackage().getImplementationVersion();
        assertNotNull(version, artifact + " version could not be determined");
        assertTrue(compare(version, MIN_FIXED_VERSION) >= 0,
                artifact + " " + version + " is below " + Arrays.toString(MIN_FIXED_VERSION));
    }

    private static int compare(String version, int[] minimum) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            final int part = i < parts.length && parts[i].matches("\\d+") ? Integer.parseInt(parts[i]) : 0;
            if (part != minimum[i]) {
                return Integer.compare(part, minimum[i]);
            }
        }
        return 0;
    }
}
