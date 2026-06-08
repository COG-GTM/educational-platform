package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the Spring Boot plugin enables full environment abstraction
 * and property resolution. bootRun supports command-line arguments, system
 * properties, and application.properties — these tests verify the property
 * source hierarchy is correctly initialised when the plugin is active.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "test.custom.property=bootRunVerification",
                "test.numeric.property=42"
        }
)
class SpringBootEnvironmentConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void environment_shouldResolveCustomTestProperty() {
        assertThat(environment.getProperty("test.custom.property"))
                .as("@SpringBootTest(properties=...) must inject properties into the environment")
                .isEqualTo("bootRunVerification");
    }

    @Test
    void environment_shouldResolveNumericProperty() {
        assertThat(environment.getProperty("test.numeric.property", Integer.class))
                .as("Environment must support typed property resolution")
                .isEqualTo(42);
    }

    @Test
    void environment_shouldHaveMultiplePropertySources() {
        MutablePropertySources sources = environment.getPropertySources();
        assertThat(sources.size())
                .as("Spring Boot must populate multiple property sources (system, env, application.properties)")
                .isGreaterThan(3);
    }

    @Test
    void environment_shouldContainSystemProperties() {
        assertThat(environment.getProperty("java.version"))
                .as("System properties must be accessible via the environment (JVM version for bootRun)")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    void environment_shouldResolveSpringDatasourceProperties() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("Datasource URL from application.properties must be resolvable")
                .isNotNull();
    }

    @Test
    void environment_shouldSupportPlaceholderResolution() {
        // Spring Boot enables ${...} placeholder resolution by default
        assertThat(environment.resolvePlaceholders("${java.version}"))
                .as("Environment must resolve ${...} placeholders for bootRun property interpolation")
                .isNotNull()
                .doesNotContain("${");
    }

    @Test
    void environment_shouldContainApplicationSecurityPropertySource() {
        // @PropertySource("application-security.properties") adds a named property source
        MutablePropertySources sources = environment.getPropertySources();
        boolean hasSecuritySource = false;
        for (var ps : sources) {
            if (ps.getName().contains("application-security")) {
                hasSecuritySource = true;
                break;
            }
        }
        assertThat(hasSecuritySource)
                .as("@PropertySource('application-security.properties') must be loaded into environment")
                .isTrue();
    }

    @Test
    void environment_shouldHavePropertySourcePrecedence_testOverApplication() {
        // Test properties should override application.properties (Spring Boot behavior)
        assertThat(environment.getProperty("test.custom.property"))
                .as("Test properties must take precedence (higher priority) over application.properties defaults")
                .isEqualTo("bootRunVerification");
    }
}
