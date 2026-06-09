package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin version aligns with the runtime version.
 * The gradle/libs.versions.toml defines the plugin version using the 'spring'
 * version reference; this test ensures the resolved runtime matches expectations.
 */
class SpringBootPluginVersionTest {

    @Test
    void springBootVersion_shouldBe4x() {
        // The libs.versions.toml sets spring = "4.0.1"
        String version = SpringBootVersion.getVersion();
        assertThat(version)
                .as("Spring Boot runtime version should be 4.x as declared in libs.versions.toml")
                .startsWith("4.");
    }

    @Test
    void springBootVersion_shouldNotBeNull() {
        // If the Spring Boot plugin is not applied, this class may not resolve at all,
        // but if it does, the version must be resolvable.
        assertThat(SpringBootVersion.getVersion())
                .as("Spring Boot version must be resolvable when plugin is applied")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void springBootVersion_shouldMatchExactDeclaredVersion() {
        // libs.versions.toml declares spring = "4.0.1"
        assertThat(SpringBootVersion.getVersion())
                .as("Spring Boot runtime version must match the exact version declared in libs.versions.toml")
                .isEqualTo("4.0.1");
    }

    @Test
    void springBootVersion_shouldFollowSemanticVersioningFormat() {
        String version = SpringBootVersion.getVersion();
        assertThat(version)
                .as("Spring Boot version must follow semantic versioning (major.minor.patch)")
                .matches("\\d+\\.\\d+\\.\\d+.*");
    }
}
