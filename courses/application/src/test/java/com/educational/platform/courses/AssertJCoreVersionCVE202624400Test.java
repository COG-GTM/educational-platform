package com.educational.platform.courses;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against regressing CVE-2026-24400 (XXE in AssertJ XML assertions).
 * This module declares assertj-core directly via the version catalog
 * ({@code libs.versions.assertj}), so the resolved version is controlled by
 * {@code gradle/libs.versions.toml}. It must stay at 3.27.7 or above.
 */
class AssertJCoreVersionCVE202624400Test {

    private static final int[] MINIMUM_SAFE_VERSION = {3, 27, 7};

    @Test
    void resolvedAssertJCoreVersion_isAtLeast3_27_7() {
        // given
        final String version = resolvedAssertJVersion();

        // then
        assertThat(version).as("assertj-core version parsed from jar location").isNotBlank();
        assertThat(isAtLeast(version, MINIMUM_SAFE_VERSION))
                .as("assertj-core %s must be >= 3.27.7 (CVE-2026-24400)", version)
                .isTrue();
    }

    private static String resolvedAssertJVersion() {
        final String jarPath = Assertions.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        final Matcher matcher = Pattern.compile("assertj-core-([0-9][0-9.]*)\\.jar").matcher(jarPath);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static boolean isAtLeast(String version, int[] minimum) {
        final String[] parts = version.split("[.-]");
        for (int i = 0; i < minimum.length; i++) {
            final int part = i < parts.length ? Integer.parseInt(parts[i]) : 0;
            if (part != minimum[i]) {
                return part > minimum[i];
            }
        }
        return true;
    }
}
