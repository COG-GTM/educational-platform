package com.educational.platform.dependencies;

import com.fasterxml.jackson.core.Version;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the {@code jackson-2-bom.version} override in the root build script:
 * jackson-databind 2.20.1 (managed by Spring Boot 4.0.1) is vulnerable to CVE-2026-59888,
 * fixed in 2.21.4.
 */
class Jackson2VersionTest {

    private static final Version MINIMUM_SAFE_VERSION = new Version(2, 21, 4, null, null, null);

    @Test
    void jacksonDatabind_isAtLeastMinimumSafeVersion() {
        // given
        final Version databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertThat(databind.getMajorVersion()).isEqualTo(2);
        assertThat(databind.compareTo(MINIMUM_SAFE_VERSION))
                .as("jackson-databind %s must be >= %s (CVE-2026-59888)", databind, MINIMUM_SAFE_VERSION)
                .isGreaterThanOrEqualTo(0);
    }

    @Test
    void jacksonCore_matchesDatabindVersion() {
        // given
        final Version core = com.fasterxml.jackson.core.json.PackageVersion.VERSION;
        final Version databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertThat(core).isEqualTo(databind);
    }
}
