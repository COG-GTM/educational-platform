package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the application entry point does not use {@code @Import},
 * {@code @ImportResource}, or {@code @ImportAutoConfiguration} annotations.
 * These annotations bypass Spring Boot's auto-configuration mechanism and
 * create tight coupling between the composition root and specific configuration
 * classes. With the Spring Boot plugin applied, auto-configuration should be
 * the primary mechanism for assembling the application context.
 * <p>
 * Complements {@link ApplicationAnnotationCompletenessTest} which guards the
 * exact annotation set, and {@link EducationalPlatformApplicationAnnotationTest}
 * which validates specific annotation attributes. This test explicitly
 * guards against import annotations that would bypass auto-configuration.
 */
class ApplicationClassNoExplicitImportTest {

    @Test
    void applicationClass_shouldNotUseImportAnnotation() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.context.annotation.Import.class))
                .as("@Import must NOT be on the application class — "
                        + "auto-configuration via @SpringBootApplication is the preferred mechanism; "
                        + "@Import creates tight coupling to specific configuration classes")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotUseImportResourceAnnotation() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.context.annotation.ImportResource.class))
                .as("@ImportResource must NOT be on the application class — "
                        + "XML-based configuration is not used in this project; "
                        + "all configuration is Java-based and auto-detected")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotUseImportAutoConfiguration() {
        // ImportAutoConfiguration is in spring-boot-autoconfigure
        boolean hasImportAutoConfig = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getName)
                .anyMatch(name -> name.contains("ImportAutoConfiguration"));
        assertThat(hasImportAutoConfig)
                .as("@ImportAutoConfiguration must NOT be on the application class — "
                        + "it is intended for test slices, not the main application class")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotUseConfigurationPropertiesScan() {
        boolean hasConfigPropsScan = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .anyMatch(name -> name.equals("ConfigurationPropertiesScan"));
        assertThat(hasConfigPropsScan)
                .as("@ConfigurationPropertiesScan must NOT be on the application class — "
                        + "the project does not use @ConfigurationProperties classes; "
                        + "adding this annotation would trigger unnecessary classpath scanning")
                .isFalse();
    }

    @Test
    void applicationClass_annotations_shouldNotReferenceSpecificBeans() {
        Set<String> annotationNames = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        assertThat(annotationNames)
                .as("Application class annotations must not include bean-specific references "
                        + "like @EnableJpaRepositories or @EntityScan — these are handled by "
                        + "auto-configuration when the Spring Boot plugin is applied")
                .doesNotContain("EnableJpaRepositories", "EntityScan",
                        "EnableTransactionManagement", "EnableWebMvc");
    }
}
