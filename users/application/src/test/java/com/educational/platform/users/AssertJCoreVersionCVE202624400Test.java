package com.educational.platform.users;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AssertJCoreVersionCVE202624400Test {

    @Test
    void resolvedAssertJCoreVersion_isAtLeast3_27_7() {
        // given
        final String jarPath = Assertions.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        // when
        final Matcher matcher = Pattern.compile("assertj-core-([0-9]+\\.[0-9]+\\.[0-9]+)\\.jar$").matcher(jarPath);

        // then
        assertThat(matcher.find()).as("assertj-core version parsed from jar location: %s", jarPath).isTrue();
        final String version = matcher.group(1);
        assertThat(Runtime.Version.parse(version).compareTo(Runtime.Version.parse("3.27.7")))
                .as("assertj-core %s must be >= 3.27.7 (CVE-2026-24400)", version)
                .isGreaterThanOrEqualTo(0);
    }
}
