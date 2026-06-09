package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PropertySourceFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the @PropertySource annotation attributes on the application entry point.
 * Changing ignoreResourceNotFound to true would silently skip loading security properties,
 * causing Spring Security to run with defaults. These tests guard against accidental
 * attribute modifications that would degrade security configuration.
 */
class PropertySourceAttributeValidationTest {

    @Test
    void propertySource_ignoreResourceNotFound_shouldBeFalse() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        assertThat(ps.ignoreResourceNotFound())
                .as("ignoreResourceNotFound must be false (default) so a missing "
                        + "application-security.properties causes a startup failure rather than silent misconfiguration")
                .isFalse();
    }

    @Test
    void propertySource_encoding_shouldBeEmpty() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        assertThat(ps.encoding())
                .as("encoding should be empty (default platform encoding) unless explicit charset is required")
                .isEmpty();
    }

    @Test
    void propertySource_name_shouldBeEmpty() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        assertThat(ps.name())
                .as("name should be empty (auto-generated from location) to avoid naming conflicts")
                .isEmpty();
    }

    @Test
    void propertySource_factory_shouldBeDefaultPropertySourceFactory() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        assertThat(ps.factory())
                .as("factory must be the default PropertySourceFactory for standard .properties file loading")
                .isEqualTo(PropertySourceFactory.class);
    }

    @Test
    void propertySource_shouldDeclareExactlyOneLocation() {
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);
        assertThat(ps).isNotNull();
        assertThat(ps.value())
                .as("@PropertySource must declare exactly one location (application-security.properties)")
                .hasSize(1);
    }
}
