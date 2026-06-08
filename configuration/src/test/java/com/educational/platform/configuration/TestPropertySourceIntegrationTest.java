package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that @TestPropertySource (a distinct mechanism from
 * @SpringBootTest(properties=...)) works correctly with the Spring Boot
 * plugin and starter-test dependency. @TestPropertySource loads properties
 * from a location or inline values and takes higher precedence than
 * application.properties — essential for test isolation.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@TestPropertySource(properties = {
        "test.isolation.key=isolated-value",
        "test.isolation.number=99",
        "test.isolation.override=from-test-property-source"
})
class TestPropertySourceIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void testPropertySource_shouldInjectStringProperty() {
        assertThat(environment.getProperty("test.isolation.key"))
                .as("@TestPropertySource must inject custom string properties")
                .isEqualTo("isolated-value");
    }

    @Test
    void testPropertySource_shouldInjectNumericProperty() {
        assertThat(environment.getProperty("test.isolation.number", Integer.class))
                .as("@TestPropertySource must support type-converted numeric properties")
                .isEqualTo(99);
    }

    @Test
    void testPropertySource_shouldCoexistWithApplicationProperties() {
        assertThat(environment.getProperty("spring.datasource.url"))
                .as("application.properties values must still be resolvable alongside @TestPropertySource")
                .isNotNull()
                .contains("h2");
    }

    @Test
    void testPropertySource_shouldCoexistWithPropertySourceAnnotation() {
        assertThat(environment.getProperty("com.educational.platform.security.enabled"))
                .as("@PropertySource on the application class must still load when @TestPropertySource is present")
                .isEqualTo("true");
    }

    @Test
    void testPropertySource_shouldBeHigherPrecedenceThanDefaults() {
        assertThat(environment.getProperty("test.isolation.override"))
                .as("@TestPropertySource properties must take precedence over defaults")
                .isEqualTo("from-test-property-source");
    }
}
