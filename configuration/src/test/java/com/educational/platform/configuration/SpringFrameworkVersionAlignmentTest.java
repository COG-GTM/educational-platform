package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.core.SpringVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Framework version on the classpath aligns with
 * the Spring Boot 4.x plugin applied to the configuration module.
 * Spring Boot 4.0.1 requires Spring Framework 7.x; a version mismatch
 * (e.g., Spring Framework 6.x with Spring Boot 4.x) would cause
 * NoSuchMethodError or ClassNotFoundException at runtime because the
 * Boot auto-configuration relies on Framework APIs that changed between
 * major versions.
 * <p>
 * Complements {@link SpringBootPluginVersionTest} (validates Boot version)
 * and {@link RootBuildBomAlignmentTest} (validates BOM/plugin version lock).
 */
class SpringFrameworkVersionAlignmentTest {

    @Test
    void springFrameworkVersion_shouldBeResolvable() {
        String version = SpringVersion.getVersion();
        assertThat(version)
                .as("Spring Framework version must be resolvable — "
                        + "null indicates the Spring JARs are not on the classpath")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void springFrameworkVersion_shouldFollowSemanticVersioning() {
        String version = SpringVersion.getVersion();
        assertThat(version)
                .as("Spring Framework version must follow semver (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+.*");
    }

    @Test
    void springFrameworkVersion_shouldBe7x_forSpringBoot4x() {
        String version = SpringVersion.getVersion();
        assertThat(version)
                .as("Spring Framework must be 7.x when Spring Boot 4.x is applied — "
                        + "Boot 4.0.1 requires Framework 7.x for its auto-configuration "
                        + "and dependency injection APIs")
                .startsWith("7.");
    }

    @Test
    void springFrameworkVersion_shouldNotBe6x() {
        String version = SpringVersion.getVersion();
        assertThat(version)
                .as("Spring Framework 6.x is incompatible with Spring Boot 4.x — "
                        + "the BOM should resolve Framework 7.x via the 'spring' version key")
                .doesNotStartWith("6.");
    }

    @Test
    void springFrameworkVersion_shouldNotBe5x() {
        String version = SpringVersion.getVersion();
        assertThat(version)
                .as("Spring Framework 5.x is incompatible with Spring Boot 4.x — "
                        + "this would indicate a broken BOM or classpath configuration")
                .doesNotStartWith("5.");
    }
}
