package com.educational.platform.dependencies;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Jackson 2.x BOM override ({@code jackson-2-bom.version = 2.21.5}) that remediates
 * CVE-2026-54512, CVE-2026-54513, CVE-2026-54514, CVE-2026-54515, CVE-2026-59888,
 * GHSA-72hv-8253-57qq and SNYK-JAVA-COMFASTERXMLJACKSONCORE-15907551.
 * CVE-2026-54515 requires the highest fix version, 2.21.5.
 */
class Jackson2VersionCVETest {

    private static final int REQUIRED_MAJOR = 2;
    private static final int REQUIRED_MINOR = 21;
    private static final int REQUIRED_PATCH = 5;

    @Test
    void jacksonDatabind2_versionIsAtLeast2_21_5() {
        // given
        final var version = com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;

        // then
        assertThat(version.getMajorVersion()).isEqualTo(REQUIRED_MAJOR);
        assertThat(isAtLeast(version.getMajorVersion(), version.getMinorVersion(), version.getPatchLevel()))
                .as("jackson-databind %s must be >= 2.21.5 (CVE-2026-54512/54513/54514/54515/59888)", version)
                .isTrue();
    }

    @Test
    void jacksonCore2_versionIsAtLeast2_21_5() {
        // given
        final var version = com.fasterxml.jackson.core.json.PackageVersion.VERSION;

        // then
        assertThat(version.getMajorVersion()).isEqualTo(REQUIRED_MAJOR);
        assertThat(isAtLeast(version.getMajorVersion(), version.getMinorVersion(), version.getPatchLevel()))
                .as("jackson-core %s must be >= 2.21.5 (GHSA-72hv-8253-57qq, SNYK-...-15907551)", version)
                .isTrue();
    }

    private static boolean isAtLeast(int major, int minor, int patch) {
        if (major != REQUIRED_MAJOR) {
            return major > REQUIRED_MAJOR;
        }
        if (minor != REQUIRED_MINOR) {
            return minor > REQUIRED_MINOR;
        }
        return patch >= REQUIRED_PATCH;
    }
}
