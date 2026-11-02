package com.educational.platform.dependency;

import ch.qos.logback.core.CoreConstants;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2026-1225 (SNYK-JAVA-CHQOSLOGBACK-15062482), fixed in logback-core 1.5.25.
 */
class LogbackVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {1, 5, 25};
    private static final Pattern JAR_VERSION = Pattern.compile("logback-core-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar");

    @Test
    void logbackCoreOnClasspathIsPatched() throws URISyntaxException {
        Path jar = Path.of(CoreConstants.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Matcher matcher = JAR_VERSION.matcher(jar.getFileName().toString());
        assertTrue(matcher.matches(), "Unexpected logback-core artifact: " + jar);

        int[] actual = {
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        };
        assertTrue(Arrays.compare(actual, MINIMUM_SAFE_VERSION) >= 0,
                "logback-core " + jar.getFileName() + " is vulnerable to CVE-2026-1225; require >= 1.5.25");
    }
}
