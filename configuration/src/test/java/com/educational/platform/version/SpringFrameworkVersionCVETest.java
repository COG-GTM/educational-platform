package com.educational.platform.version;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the Spring Framework version override against regressions.
 *
 * <p>Spring Framework must be at least 7.0.8 to include the fixes for
 * CVE-2026-41851, CVE-2026-41850, CVE-2026-41840, CVE-2026-22737 and the other
 * CVEs remediated in the 7.0.3&ndash;7.0.8 patch releases. The version is pinned
 * via the {@code spring-framework.version} BOM property in the root build; if
 * that override is removed, the Spring Boot 4.0.1 BOM falls back to the
 * vulnerable 7.0.2.
 */
public class SpringFrameworkVersionCVETest {

    private static final int REQUIRED_MAJOR = 7;
    private static final int REQUIRED_MINOR = 0;
    private static final int REQUIRED_PATCH = 8;

    @Test
    void springFrameworkVersion_isAtLeast_7_0_8() {
        // given
        final String version = SpringVersion.getVersion();
        assertNotNull(version, "Spring Framework version should be resolvable from spring-core");

        // when
        final String[] parts = version.split("[.-]");
        final int major = Integer.parseInt(parts[0]);
        final int minor = Integer.parseInt(parts[1]);
        final int patch = Integer.parseInt(parts[2]);

        // then
        assertTrue(
                major > REQUIRED_MAJOR
                        || (major == REQUIRED_MAJOR && minor > REQUIRED_MINOR)
                        || (major == REQUIRED_MAJOR && minor == REQUIRED_MINOR && patch >= REQUIRED_PATCH),
                "Spring Framework version " + version + " is older than 7.0.8 and is exposed to "
                        + "CVE-2026-41851, CVE-2026-41850 and other CVEs fixed in 7.0.x patch releases");
    }
}
