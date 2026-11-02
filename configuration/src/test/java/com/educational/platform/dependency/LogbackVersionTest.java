package com.educational.platform.dependency;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.CoreConstants;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against CVE-2026-1225 (SNYK-JAVA-CHQOSLOGBACK-15062482), fixed in logback-core 1.5.25.
 */
class LogbackVersionTest {

    private static final int[] MINIMUM_SAFE_VERSION = {1, 5, 25};
    private static final Pattern JAR_VERSION = Pattern.compile("(logback-(?:core|classic))-(\\d+)\\.(\\d+)\\.(\\d+)\\.jar");

    @Test
    void logbackCoreOnClasspathIsPatched() throws URISyntaxException {
        int[] actual = versionOf(CoreConstants.class, "logback-core");
        assertTrue(Arrays.compare(actual, MINIMUM_SAFE_VERSION) >= 0,
                "logback-core logback-core-" + actual[0] + "." + actual[1] + "." + actual[2]
                        + ".jar is vulnerable to CVE-2026-1225; require >= 1.5.25");
    }

    @Test
    void logbackClassicOnClasspathIsPatched() throws URISyntaxException {
        int[] actual = versionOf(LoggerContext.class, "logback-classic");
        assertTrue(Arrays.compare(actual, MINIMUM_SAFE_VERSION) >= 0,
                "logback-classic logback-classic-" + actual[0] + "." + actual[1] + "." + actual[2]
                        + ".jar is vulnerable to CVE-2026-1225; require >= 1.5.25");
    }

    @Test
    void logbackClassicAndCoreVersionsMatch() throws URISyntaxException {
        int[] core = versionOf(CoreConstants.class, "logback-core");
        int[] classic = versionOf(LoggerContext.class, "logback-classic");
        assertArrayEquals(core, classic,
                "logback-classic and logback-core versions must match: "
                        + Arrays.toString(classic) + " != " + Arrays.toString(core));
    }

    private static int[] versionOf(Class<?> anchor, String artifact) throws URISyntaxException {
        Path jar = Path.of(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        Matcher matcher = JAR_VERSION.matcher(jar.getFileName().toString());
        assertTrue(matcher.matches() && matcher.group(1).equals(artifact),
                "Unexpected " + artifact + " artifact: " + jar);

        return new int[]{
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                Integer.parseInt(matcher.group(4))
        };
    }
}
