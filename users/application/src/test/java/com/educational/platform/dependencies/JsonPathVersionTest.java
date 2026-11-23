package com.educational.platform.dependencies;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against CVE-2023-51074 (stack-based buffer overflow in json-path < 2.9.0).
 */
class JsonPathVersionTest {

    private static final String MINIMUM_SAFE_VERSION = "2.9.0";

    @Test
    void jsonPathOnClasspathIsNotVulnerableToCve202351074() throws Exception {
        final String version = resolvedJsonPathVersion();

        assertThat(compare(version, MINIMUM_SAFE_VERSION))
                .as("json-path %s is vulnerable to CVE-2023-51074, need >= %s", version, MINIMUM_SAFE_VERSION)
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void jsonPathOnClasspathMatchesVersionCatalog() throws Exception {
        final String expected = System.getProperty("expected.json-path.version");
        if (expected != null) {
            assertThat(resolvedJsonPathVersion()).isEqualTo(expected);
        }
    }

    private static String resolvedJsonPathVersion() throws Exception {
        final File jar = new File(JsonPath.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarFile jarFile = new JarFile(jar)) {
            return jarFile.getManifest().getMainAttributes().getValue("Bundle-Version");
        }
    }

    private static int compare(String left, String right) {
        final String[] l = left.split("[.-]");
        final String[] r = right.split("[.-]");
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            final int a = i < l.length ? parse(l[i]) : 0;
            final int b = i < r.length ? parse(r[i]) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    private static int parse(String part) {
        return part.chars().allMatch(Character::isDigit) && !part.isEmpty() ? Integer.parseInt(part) : 0;
    }
}
