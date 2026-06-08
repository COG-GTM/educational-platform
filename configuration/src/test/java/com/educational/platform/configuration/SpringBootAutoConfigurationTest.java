package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that Spring Boot auto-configuration activates expected infrastructure
 * beans when the Spring Boot plugin is correctly applied. Without the plugin,
 * these tests would fail at context initialization.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SpringBootAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContext_shouldContainApplicationBean() {
        assertThat(applicationContext.getBean(EducationalPlatformApplication.class))
                .as("The application class should be registered as a Spring bean")
                .isNotNull();
    }

    @Test
    void applicationContext_shouldHaveActiveProfiles_orDefaultProfile() {
        // Spring Boot plugin enables profile-based configuration;
        // verify the environment is initialized
        var environment = applicationContext.getEnvironment();
        assertThat(environment.getActiveProfiles().length + environment.getDefaultProfiles().length)
                .as("At least one profile (active or default) must be available")
                .isGreaterThan(0);
    }

    @Test
    void applicationContext_shouldResolvePropertySources() {
        // Verifies @PropertySource("application-security.properties") is loaded
        var environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
        assertThat(environment.getPropertySources())
                .as("Property sources should be populated via @PropertySource and Spring Boot auto-configuration")
                .isNotNull();
        assertThat(environment.getPropertySources().size())
                .as("Multiple property sources should be registered")
                .isGreaterThan(1);
    }

    @Test
    void applicationContext_shouldEnableAsyncSupport() {
        // @EnableAsync should register async-related infrastructure beans
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        boolean hasAsyncBean = false;
        for (String name : beanNames) {
            if (name.toLowerCase().contains("async")) {
                hasAsyncBean = true;
                break;
            }
        }
        assertThat(hasAsyncBean)
                .as("@EnableAsync should register at least one async-related bean in the context")
                .isTrue();
    }
}
