package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the full Spring application context loads successfully.
 * This test validates that the Spring Boot plugin is correctly applied
 * to the configuration module: without the plugin, the application
 * context cannot bootstrap via {@code @SpringBootTest}.
 */
@SpringBootTest(
        classes = EducationalPlatformApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class EducationalPlatformApplicationTest {

    @Test
    void contextLoads() {
        // If the Spring Boot plugin is missing or misconfigured,
        // this test will fail during context initialization.
    }
}
