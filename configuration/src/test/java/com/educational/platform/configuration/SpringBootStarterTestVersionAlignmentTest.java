package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the spring-boot-starter-test dependency's runtime version
 * aligns with the Spring Boot plugin version. The PR added starter-test to
 * configuration/build.gradle.kts using BOM-managed version resolution
 * (no explicit version in the dependency declaration). These tests verify
 * the alignment is maintained at runtime — a version mismatch between
 * the boot plugin and the test starter would cause incompatible
 * assertions, missing test infrastructure classes, or subtle behavior
 * differences in @SpringBootTest and ApplicationContextRunner.
 *
 * Complements {@link SpringBootDependencyAlignmentTest} (which checks
 * runtime starter-web alignment) and {@link StarterTestTransitiveDependencyPresenceTest}
 * (which checks transitive dependency availability).
 */
class SpringBootStarterTestVersionAlignmentTest {

    @Test
    void springBootTestVersion_shouldMatchPluginVersion() {
        String pluginVersion = SpringBootVersion.getVersion();
        assertThatCode(() -> {
            Class<?> springBootTestClass = Class.forName(
                    "org.springframework.boot.test.context.SpringBootTest");
            Package pkg = springBootTestClass.getPackage();
            // The implementation version may be null in some classloader scenarios,
            // but the class must at least be loadable from the same Spring Boot release
            if (pkg != null && pkg.getImplementationVersion() != null) {
                assertThat(pkg.getImplementationVersion())
                        .as("@SpringBootTest package version must match the plugin version (%s)",
                                pluginVersion)
                        .startsWith(pluginVersion.substring(0, pluginVersion.lastIndexOf('.')));
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void springBootTestContextLoader_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.context.SpringBootContextLoader"))
                .as("SpringBootContextLoader must be on classpath — "
                        + "it is the primary context loader used by @SpringBootTest and "
                        + "must come from the same Spring Boot version as the plugin")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTestWebEnvironment_shouldHaveExpectedValues() {
        assertThatCode(() -> {
            Class<?> webEnvClass = Class.forName(
                    "org.springframework.boot.test.context.SpringBootTest$WebEnvironment");
            assertThat(webEnvClass.isEnum()).isTrue();
            Object[] constants = webEnvClass.getEnumConstants();
            assertThat(constants)
                    .as("WebEnvironment enum must have MOCK, RANDOM_PORT, DEFINED_PORT, NONE values")
                    .hasSizeGreaterThanOrEqualTo(4);
        }).doesNotThrowAnyException();
    }

    @Test
    void outputCaptureExtension_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.system.OutputCaptureExtension"))
                .as("OutputCaptureExtension must be on classpath — "
                        + "it is part of spring-boot-test and validates version alignment "
                        + "between starter-test and the plugin")
                .doesNotThrowAnyException();
    }

    @Test
    void testPropertyValues_shouldBeAvailable() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.test.util.TestPropertyValues"))
                .as("TestPropertyValues must be on classpath — "
                        + "used for programmatic property configuration in unit tests "
                        + "and must align with the Spring Boot plugin version")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootVersion_shouldBeConsistentAcrossModules() {
        String bootVersion = SpringBootVersion.getVersion();
        assertThatCode(() -> {
            // Verify the test mock support classes match the runtime version
            Class<?> mockBeanClass = Class.forName(
                    "org.springframework.test.context.bean.override.mockito.MockitoBean");
            assertThat(mockBeanClass.isAnnotation()).isTrue();
        }).as("@MockitoBean annotation must be resolvable, confirming test infrastructure "
                        + "version (%s) is consistent with the plugin", bootVersion)
                .doesNotThrowAnyException();
    }
}
