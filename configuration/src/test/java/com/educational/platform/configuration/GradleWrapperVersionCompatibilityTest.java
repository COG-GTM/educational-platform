package com.educational.platform.configuration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Gradle wrapper version is compatible with the
 * Spring Boot 4.x Gradle plugin. Spring Boot 4.x requires Gradle 8.5+;
 * using an older Gradle wrapper would cause the plugin to fail with
 * incompatible API errors during bootRun/bootJar.
 * GradleWrapperConsistencyTest validates wrapper presence; this test
 * validates version compatibility with the applied Spring Boot plugin.
 */
class GradleWrapperVersionCompatibilityTest {

    private static Path projectRoot;
    private static String wrapperProperties;

    @BeforeAll
    static void loadWrapperProperties() throws IOException {
        Path dir = Path.of(System.getProperty("user.dir"));
        while (dir != null && !Files.exists(dir.resolve("settings.gradle.kts"))) {
            dir = dir.getParent();
        }
        assertThat(dir)
                .as("Project root containing settings.gradle.kts must be reachable")
                .isNotNull();
        projectRoot = dir;
        wrapperProperties = Files.readString(
                projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties"));
    }

    @Test
    void gradleVersion_shouldBeAtLeast8_5_forSpringBoot4Compatibility() {
        String version = extractGradleVersion();
        assertThat(version)
                .as("Gradle wrapper version must be extractable from distributionUrl")
                .isNotNull();

        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        assertThat(major * 100 + minor)
                .as("Gradle version %s must be >= 8.5 for Spring Boot 4.x plugin compatibility", version)
                .isGreaterThanOrEqualTo(805);
    }

    @Test
    void gradleVersion_shouldFollowSemanticVersioning() {
        String version = extractGradleVersion();
        assertThat(version)
                .as("Gradle version in wrapper properties must follow semver format")
                .matches("\\d+\\.\\d+(\\.\\d+)?(-\\w+)?");
    }

    @Test
    void distributionUrl_shouldReferenceGradleDistribution() {
        assertThat(wrapperProperties)
                .as("distributionUrl must reference a Gradle distribution ZIP")
                .containsPattern("distributionUrl.*gradle-\\d+\\.\\d+.*\\.zip");
    }

    @Test
    void distributionUrl_shouldUseBinDistribution() {
        assertThat(wrapperProperties)
                .as("distributionUrl should use -bin distribution (not -all) "
                        + "to minimize download size in CI and bootRun")
                .containsPattern("gradle-[\\d.]+-bin\\.zip");
    }

    @Test
    void wrapperProperties_shouldValidateDistributionUrl() {
        assertThat(wrapperProperties)
                .as("validateDistributionUrl must be true for supply-chain security")
                .containsPattern("validateDistributionUrl\\s*=\\s*true");
    }

    private String extractGradleVersion() {
        Matcher m = Pattern.compile("gradle-(\\d+\\.\\d+(?:\\.\\d+)?(?:-\\w+)?)-")
                .matcher(wrapperProperties);
        return m.find() ? m.group(1) : null;
    }
}
