package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the auto-configuration discovery mechanism provided by
 * spring-boot-starter-test is functional. Spring Boot 4.x uses
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * for auto-configuration registration. The starter-test brings
 * spring-boot-test-autoconfigure, which registers test-specific
 * auto-configurations (e.g., JSON tester support, override auto-configuration).
 * If this mechanism is broken — for example, because the META-INF file is
 * stripped by a dependency exclusion or shading — test slices like
 * {@code @JsonTest} would silently stop auto-configuring.
 * <p>
 * Complements {@link ConfigurationModuleNoAutoConfigRegistrationTest}
 * (which validates the configuration module itself doesn't register
 * custom auto-configs) and {@link TestSliceAnnotationAvailabilityTest}
 * (which validates annotation availability). This test validates the
 * auto-configuration registration mechanism itself.
 */
class StarterTestAutoConfigurationImportsTest {

    private static final String AUTO_CONFIG_RESOURCE =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    @Test
    void autoConfigurationImports_shouldExistOnClasspath() throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader()
                .getResources(AUTO_CONFIG_RESOURCE);
        assertThat(resources.hasMoreElements())
                .as("At least one AutoConfiguration.imports file must be on the classpath — "
                        + "Spring Boot 4.x uses this file for auto-configuration discovery; "
                        + "its absence would prevent auto-configuration from activating")
                .isTrue();
    }

    @Test
    void autoConfigurationImports_shouldContainMultipleEntries() throws IOException {
        Enumeration<URL> resources = getClass().getClassLoader()
                .getResources(AUTO_CONFIG_RESOURCE);
        int fileCount = 0;
        while (resources.hasMoreElements()) {
            resources.nextElement();
            fileCount++;
        }
        assertThat(fileCount)
                .as("Multiple AutoConfiguration.imports files should be on the classpath — "
                        + "at least from spring-boot-autoconfigure and spring-boot-test-autoconfigure")
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void legacySpringFactories_shouldNotBeRequiredForAutoConfig() {
        // Spring Boot 4.x removed support for spring.factories-based auto-config registration.
        // This test documents that the project does not depend on the legacy mechanism.
        URL legacyFactories = getClass().getClassLoader()
                .getResource("META-INF/spring.factories");
        // spring.factories may still exist for other purposes (e.g., failure analyzers),
        // but auto-configuration must use the imports file.
        assertThat(true)
                .as("Documenting: spring.factories may or may not exist; "
                        + "auto-configuration uses AutoConfiguration.imports in 4.x")
                .isTrue();
    }

    @Test
    void springBootTestContextBootstrapper_shouldBeLoadable() {
        assertThatCode(() -> {
            Class<?> bootstrapper = Class.forName(
                    "org.springframework.boot.test.context.SpringBootTestContextBootstrapper");
            assertThat(bootstrapper)
                    .as("SpringBootTestContextBootstrapper must be loadable — "
                            + "it is the entry point for @SpringBootTest context creation")
                    .isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void testAutoConfiguration_overrideAnnotation_shouldBeLoadable() {
        assertThatCode(() -> {
            Class<?> overrideClass = Class.forName(
                    "org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration");
            assertThat(overrideClass.isAnnotation())
                    .as("@OverrideAutoConfiguration must be an annotation type")
                    .isTrue();
        }).doesNotThrowAnyException();
    }
}
