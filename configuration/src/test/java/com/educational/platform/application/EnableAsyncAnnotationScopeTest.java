package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the @EnableAsync annotation attribute that controls which
 * annotation types trigger async interception. EnableAsyncAttributeTest
 * covers mode, proxyTargetClass, and order; this test covers the
 * annotation-type scope which, if changed, would silently disable
 * async processing for standard @Async methods.
 */
class EnableAsyncAnnotationScopeTest {

    @Test
    void enableAsync_annotationAttribute_shouldBeDefaultAnnotationType() {
        EnableAsync annotation = EducationalPlatformApplication.class.getAnnotation(EnableAsync.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.annotation())
                .as("@EnableAsync annotation attribute must be the default (java.lang.annotation.Annotation) "
                        + "so that all @Async-annotated methods are intercepted for inter-module event publishing")
                .isEqualTo(Annotation.class);
    }

    @Test
    void enableAsync_shouldBeDeclaredDirectly_notInherited() {
        assertThat(EducationalPlatformApplication.class.getDeclaredAnnotation(EnableAsync.class))
                .as("@EnableAsync must be declared directly on the application class, not inherited, "
                        + "to ensure explicit async configuration ownership")
                .isNotNull();
    }

    @Test
    void enableAsync_shouldCoexistWithSpringBootApplication() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(EnableAsync.class))
                .isTrue();
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class))
                .as("@EnableAsync and @SpringBootApplication must coexist on the same class")
                .isTrue();
    }
}
