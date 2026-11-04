package com.educational.platform.dependencies;

import liquibase.Liquibase;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CVE-2022-0839 (SNYK-JAVA-ORGLIQUIBASE-2419792, XXE in changelog parsing) is fixed in liquibase-core 4.8.0.
 */
class LiquibaseVersionTest {

    private static final int[] MINIMUM_VERSION = {4, 8, 0};
    private static final Pattern JAR_VERSION = Pattern.compile("liquibase-core-(\\d+)\\.(\\d+)\\.(\\d+).*\\.jar");

    @Test
    void liquibaseCore_isAtLeastMinimumPatchedVersion() throws URISyntaxException {
        var codeSource = Liquibase.class.getProtectionDomain().getCodeSource();
        assertNotNull(codeSource, "liquibase-core location must be resolvable");
        String jarName = Path.of(codeSource.getLocation().toURI()).getFileName().toString();

        Matcher matcher = JAR_VERSION.matcher(jarName);
        assertTrue(matcher.matches(), "Unexpected liquibase-core artifact name: " + jarName);
        int[] actual = {
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        };
        assertTrue(Arrays.compare(actual, MINIMUM_VERSION) >= 0,
                "liquibase-core must be >= 4.8.0 (CVE-2022-0839), but was " + jarName);
    }
}
