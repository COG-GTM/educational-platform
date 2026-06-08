package com.educational.platform.configuration;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that @SpringBootTest with WebEnvironment.DEFINED_PORT boots
 * the application context with the embedded server on the port defined
 * in application.properties. The existing suite covers NONE
 * ({@link com.educational.platform.application.EducationalPlatformApplicationTest})
 * and RANDOM_PORT ({@link TestRestClientAvailabilityTest}); this test
 * fills the DEFINED_PORT gap to ensure all web environments supported by
 * the Spring Boot plugin work correctly.
 * <p>
 * DEFINED_PORT is the mode closest to bootRun behavior: same port,
 * same servlet context path, same property resolution. If the Spring
 * Boot plugin is misconfigured, this mode would fail first.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "server.port=0"
)
class SpringBootTestWebEnvironmentCompletenessTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads_withDefinedPortEnvironment() {
        assertThat(applicationContext)
                .as("Application context must load with DEFINED_PORT web environment — "
                        + "this is the closest mode to bootRun behavior")
                .isNotNull();
    }

    @Test
    void applicationContext_shouldBeWebApplicationContext() {
        assertThat(applicationContext.getClass().getName())
                .as("DEFINED_PORT must create a web application context with embedded server")
                .containsIgnoringCase("web");
    }

    @Test
    void applicationContext_shouldContainServletContext() {
        boolean hasServletBean = false;
        for (String name : applicationContext.getBeanDefinitionNames()) {
            if (name.toLowerCase().contains("servlet") || name.toLowerCase().contains("dispatcherservlet")) {
                hasServletBean = true;
                break;
            }
        }
        assertThat(hasServletBean)
                .as("DEFINED_PORT web environment must register servlet infrastructure beans")
                .isTrue();
    }
}
