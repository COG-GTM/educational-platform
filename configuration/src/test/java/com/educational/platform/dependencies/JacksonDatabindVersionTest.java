package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the {@code jackson-2-bom.version} override that remediates CVE-2026-54514
 * (SSRF in jackson-databind, fixed in 2.21.4).
 */
class JacksonDatabindVersionTest {

    private static final Version MINIMUM_SAFE_VERSION = new Version(2, 21, 4, null, "com.fasterxml.jackson.core", "jackson-databind");

    @Test
    void jacksonDatabind_isAtLeastMinimumSafeVersion() {
        // given
        final Version version = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertThat(version.getMajorVersion()).isEqualTo(MINIMUM_SAFE_VERSION.getMajorVersion());
        assertThat(version.compareTo(MINIMUM_SAFE_VERSION))
                .as("jackson-databind %s must be >= %s (CVE-2026-54514)", version, MINIMUM_SAFE_VERSION)
                .isGreaterThanOrEqualTo(0);
    }
}
