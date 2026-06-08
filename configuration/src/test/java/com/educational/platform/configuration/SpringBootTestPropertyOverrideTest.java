package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the spring-boot-starter-test dependency enables
 * property overrides via @SpringBootTest(properties = ...).
 * This is a core testing capability provided by the starter-test
 * dependency added in this PR. Without it, @SpringBootTest would
 * not be available and property-based test isolation would be impossible.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "test.custom.property=overridden-value",
                "test.custom.number=42",
                "test.custom.flag=true"
        }
)
class SpringBootTestPropertyOverrideTest {

    @Autowired
    private Environment environment;

    @Test
    void customProperty_shouldBeOverriddenByTestAnnotation() {
        assertThat(environment.getProperty("test.custom.property"))
                .as("@SpringBootTest(properties) must inject custom properties into the Environment")
                .isEqualTo("overridden-value");
    }

    @Test
    void numericProperty_shouldBeResolvable() {
        assertThat(environment.getProperty("test.custom.number", Integer.class))
                .as("Numeric properties must be resolvable with type conversion via starter-test")
                .isEqualTo(42);
    }

    @Test
    void booleanProperty_shouldBeResolvable() {
        assertThat(environment.getProperty("test.custom.flag", Boolean.class))
                .as("Boolean properties must be resolvable with type conversion via starter-test")
                .isTrue();
    }

    @Test
    void nonOverriddenProperty_shouldRetainOriginalValue() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("Properties not overridden in @SpringBootTest should retain their original value")
                .isNotNull()
                .contains("h2");
    }

    @Test
    void securityProperty_shouldRetainOriginalValue() {
        assertThat(environment.getProperty("com.educational.platform.security.enabled", Boolean.class))
                .as("Security property from @PropertySource should be loadable and retain its value")
                .isTrue();
    }
}
