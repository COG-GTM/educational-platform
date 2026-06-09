package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests validating the annotation configuration of the application
 * entry point via reflection. No Spring context is loaded.
 * These guard against accidental removal or misconfiguration of critical
 * annotations required for bootRun / bootJar to function correctly.
 */
class EducationalPlatformApplicationAnnotationTest {

    @Test
    void applicationClass_shouldBeAnnotatedWithSpringBootApplication() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(SpringBootApplication.class))
                .as("@SpringBootApplication is required for the Spring Boot plugin")
                .isTrue();
    }

    @Test
    void applicationClass_shouldBeAnnotatedWithEnableAsync() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(EnableAsync.class))
                .as("@EnableAsync is required for async event publishing between bounded contexts")
                .isTrue();
    }

    @Test
    void applicationClass_shouldBeAnnotatedWithPropertySource() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(PropertySource.class))
                .as("@PropertySource is required to load security configuration")
                .isTrue();
    }

    @Test
    void propertySource_shouldReferenceApplicationSecurityProperties() {
        // given
        PropertySource ps = EducationalPlatformApplication.class.getAnnotation(PropertySource.class);

        // then
        assertThat(ps).isNotNull();
        assertThat(ps.value())
                .as("@PropertySource must reference application-security.properties for Spring Security")
                .containsExactly("application-security.properties");
    }

    @Test
    void springBootApplication_shouldNotExcludeAutoConfigurations() {
        // given
        SpringBootApplication sba = EducationalPlatformApplication.class.getAnnotation(SpringBootApplication.class);

        // then
        assertThat(sba).isNotNull();
        assertThat(sba.exclude())
                .as("No auto-configurations should be excluded from the entry point")
                .isEmpty();
        assertThat(sba.excludeName())
                .as("No auto-configurations should be excluded by name from the entry point")
                .isEmpty();
    }

    @Test
    void springBootApplication_shouldUseScanBasePackageDefaults() {
        // given
        SpringBootApplication sba = EducationalPlatformApplication.class.getAnnotation(SpringBootApplication.class);

        // then — default empty means scan from the annotated class's package
        assertThat(sba).isNotNull();
        assertThat(sba.scanBasePackages())
                .as("scanBasePackages should be empty (default: scan from annotated class package)")
                .isEmpty();
        assertThat(sba.scanBasePackageClasses())
                .as("scanBasePackageClasses should be empty (default: scan from annotated class package)")
                .isEmpty();
    }
}
