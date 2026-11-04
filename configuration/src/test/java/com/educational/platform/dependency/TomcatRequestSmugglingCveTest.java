package com.educational.platform.dependency;

import org.apache.catalina.util.ServerInfo;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2023-46589 (HTTP request smuggling via oversized trailer headers) affects
 * tomcat-embed-core before 10.1.16 and 11.0.0-M1 to 11.0.0-M10.
 */
class TomcatRequestSmugglingCveTest {

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(-\\S+)?");

    @Test
    void embeddedTomcat_isNotVulnerableToCve202346589() {
        String serverInfo = ServerInfo.getServerInfo();

        assertFalse(isVulnerable(serverInfo),
                serverInfo + " is vulnerable to CVE-2023-46589; tomcat-embed-core 10.1.16 or 11.0.0 (GA) or later is required");
    }

    @Test
    void isVulnerable_matchesAdvisoryRanges() {
        assertTrue(isVulnerable("Apache Tomcat/10.1.8"));
        assertTrue(isVulnerable("Apache Tomcat/10.1.15"));
        assertTrue(isVulnerable("Apache Tomcat/10.0.27"));
        assertTrue(isVulnerable("Apache Tomcat/11.0.0-M10"));
        assertFalse(isVulnerable("Apache Tomcat/10.1.16"));
        assertFalse(isVulnerable("Apache Tomcat/11.0.0-M11"));
        assertFalse(isVulnerable("Apache Tomcat/11.0.0"));
        assertFalse(isVulnerable("Apache Tomcat/11.0.15"));
    }

    static boolean isVulnerable(String serverInfo) {
        Matcher matcher = VERSION.matcher(serverInfo);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Cannot parse Tomcat version from: " + serverInfo);
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String qualifier = matcher.group(4);

        if (major < 10 || (major == 10 && minor < 1)) {
            return true;
        }
        if (major == 10) {
            return minor == 1 && patch < 16;
        }
        if (major == 11 && minor == 0 && patch == 0 && qualifier != null && qualifier.startsWith("-M")) {
            return Integer.parseInt(qualifier.substring(2)) < 11;
        }
        return false;
    }
}
