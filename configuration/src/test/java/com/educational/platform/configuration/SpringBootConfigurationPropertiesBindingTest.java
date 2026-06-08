package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot plugin enables @ConfigurationProperties
 * type-safe binding — a key Spring Boot feature for externalizing configuration.
 * Without the Spring Boot plugin, @ConfigurationProperties beans are not
 * auto-detected and bound, causing configuration values to remain at defaults.
 * Existing tests cover @PropertySource and Environment; this test covers
 * the structured binding mechanism that Spring Boot adds on top.
 */
@SpringBootTest(
        classes = {EducationalPlatformApplication.class,
                SpringBootConfigurationPropertiesBindingTest.PropertiesTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "test.binding.name=educational-platform",
                "test.binding.version=1",
                "test.binding.enabled=true"
        }
)
class SpringBootConfigurationPropertiesBindingTest {

    @Autowired
    private TestBindingProperties testBindingProperties;

    @Autowired
    private Environment environment;

    @Test
    void configurationProperties_shouldBindStringProperty() {
        assertThat(testBindingProperties.getName())
                .as("@ConfigurationProperties must bind string values from test properties")
                .isEqualTo("educational-platform");
    }

    @Test
    void configurationProperties_shouldBindIntegerProperty() {
        assertThat(testBindingProperties.getVersion())
                .as("@ConfigurationProperties must bind integer values with type conversion")
                .isEqualTo(1);
    }

    @Test
    void configurationProperties_shouldBindBooleanProperty() {
        assertThat(testBindingProperties.isEnabled())
                .as("@ConfigurationProperties must bind boolean values with type conversion")
                .isTrue();
    }

    @Test
    void configurationProperties_shouldReturnDefaultForUnsetProperty() {
        assertThat(testBindingProperties.getDescription())
                .as("Unset @ConfigurationProperties fields must remain at their default (null)")
                .isNull();
    }

    @Test
    void configurationPropertiesAnnotation_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.context.properties.ConfigurationProperties"))
                .as("@ConfigurationProperties must be available on the classpath "
                        + "for type-safe configuration binding")
                .doesNotThrowAnyException();
    }

    @Test
    void enableConfigurationProperties_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName(
                "org.springframework.boot.context.properties.EnableConfigurationProperties"))
                .as("@EnableConfigurationProperties must be available for activating "
                        + "@ConfigurationProperties processing")
                .doesNotThrowAnyException();
    }

    @TestConfiguration
    @EnableConfigurationProperties(TestBindingProperties.class)
    static class PropertiesTestConfig {
    }

    @ConfigurationProperties(prefix = "test.binding")
    static class TestBindingProperties {
        private String name;
        private int version;
        private boolean enabled;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
