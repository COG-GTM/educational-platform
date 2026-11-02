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

    private static final int SAFE_MAJOR = 2;
    private static final int SAFE_MINOR = 21;
    private static final int SAFE_PATCH = 4;

    @Test
    void jacksonDatabind_isAtLeastMinimumSafeVersion() {
        // given
        final Version databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertThat(databind.getMajorVersion()).isEqualTo(SAFE_MAJOR);
        assertThat(isAtLeastMinimumSafeVersion(databind))
                .as("jackson-databind %s must be >= %d.%d.%d (CVE-2026-59888)", databind, SAFE_MAJOR, SAFE_MINOR, SAFE_PATCH)
                .isTrue();
    }

    @Test
    void jacksonCore_matchesDatabindVersion() {
        // given
        final Version core = com.fasterxml.jackson.core.json.PackageVersion.VERSION;
        final Version databind = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertThat(core.toString()).isEqualTo(databind.toString());
    }

    @Test
    void isAtLeastMinimumSafeVersion_comparesNumericComponentsIgnoringArtifactIds() {
        // then
        assertThat(isAtLeastMinimumSafeVersion(databindVersion(2, 20, 1))).isFalse();
        assertThat(isAtLeastMinimumSafeVersion(databindVersion(2, 21, 3))).isFalse();
        assertThat(isAtLeastMinimumSafeVersion(databindVersion(1, 99, 99))).isFalse();
        assertThat(isAtLeastMinimumSafeVersion(databindVersion(2, 21, 4))).isTrue();
        assertThat(isAtLeastMinimumSafeVersion(databindVersion(2, 21, 5))).isTrue();
        assertThat(isAtLeastMinimumSafeVersion(databindVersion(2, 22, 0))).isTrue();
    }

    private static Version databindVersion(final int major, final int minor, final int patch) {
        return new Version(major, minor, patch, null, "com.fasterxml.jackson.core", "jackson-databind");
    }

    private static boolean isAtLeastMinimumSafeVersion(final Version version) {
        if (version.getMajorVersion() != SAFE_MAJOR) {
            return version.getMajorVersion() > SAFE_MAJOR;
        }
        if (version.getMinorVersion() != SAFE_MINOR) {
            return version.getMinorVersion() > SAFE_MINOR;
        }
        return version.getPatchLevel() >= SAFE_PATCH;
    }
}
