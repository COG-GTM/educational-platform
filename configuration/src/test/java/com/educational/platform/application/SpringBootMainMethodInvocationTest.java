package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit test verifying that the main method correctly delegates to
 * SpringApplication.run with the application class. This ensures the
 * Spring Boot plugin's bootRun task can successfully launch the application.
 */
class SpringBootMainMethodInvocationTest {

    @Test
    void main_shouldInvokeSpringApplicationRun_withCorrectClass() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(new String[]{});

            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            ));
        }
    }

    @Test
    void main_shouldPassArguments_toSpringApplicationRun() {
        String[] args = {"--server.port=9090", "--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    eq(args)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args);

            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    eq(args)
            ));
        }
    }

    @Test
    void main_shouldHandleEmptyArgs_withoutException() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(new String[0]);

            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            ));
        }
    }
}
