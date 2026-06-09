package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the exact annotation set declared on the application entry point.
 * Individual annotation tests (EducationalPlatformApplicationAnnotationTest,
 * EnableAsyncAttributeTest, PropertySourceAttributeValidationTest) validate
 * specific annotation attributes; this test guards against annotation sprawl.
 * Adding unexpected annotations (e.g., @EnableScheduling, @EnableCaching,
 * @ComponentScan) could change auto-configuration, proxying, or scanning
 * behavior in ways that break the modular monolith architecture.
 */
class ApplicationAnnotationCompletenessTest {

    private static final Set<Class<? extends Annotation>> EXPECTED_ANNOTATIONS = Set.of(
            SpringBootApplication.class,
            EnableAsync.class,
            PropertySource.class
    );

    @Test
    void applicationClass_shouldHaveExactlyTheExpectedAnnotations() {
        Set<Class<? extends Annotation>> actualAnnotations = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredAnnotations())
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());

        assertThat(actualAnnotations)
                .as("Application class must declare exactly @SpringBootApplication, @EnableAsync, "
                        + "and @PropertySource — no more, no less. Unexpected annotations may alter "
                        + "auto-configuration, proxying, or component scanning behavior.")
                .containsExactlyInAnyOrderElementsOf(EXPECTED_ANNOTATIONS);
    }

    @Test
    void applicationClass_shouldNotBeAnnotatedWithEnableScheduling() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.scheduling.annotation.EnableScheduling.class))
                .as("@EnableScheduling must NOT be on the application class — "
                        + "scheduled tasks are not part of the current architecture")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotBeAnnotatedWithEnableCaching() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.cache.annotation.EnableCaching.class))
                .as("@EnableCaching must NOT be on the application class — "
                        + "caching should be configured explicitly per module if needed")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotDeclareExplicitComponentScan() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.context.annotation.ComponentScan.class))
                .as("@ComponentScan must NOT be declared explicitly — "
                        + "@SpringBootApplication already includes implicit component scanning "
                        + "from the annotated class's package")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotDeclareExplicitEnableAutoConfiguration() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.EnableAutoConfiguration.class))
                .as("@EnableAutoConfiguration must NOT be declared explicitly — "
                        + "@SpringBootApplication already includes it as a meta-annotation")
                .isFalse();
    }

    @Test
    void applicationClass_shouldHaveExactlyThreeDeclaredAnnotations() {
        assertThat(EducationalPlatformApplication.class.getDeclaredAnnotations())
                .as("Application class must have exactly 3 declared annotations "
                        + "(@SpringBootApplication, @EnableAsync, @PropertySource)")
                .hasSize(3);
    }
}
