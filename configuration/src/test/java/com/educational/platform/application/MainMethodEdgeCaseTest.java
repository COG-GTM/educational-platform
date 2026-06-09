package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Edge-case tests for the application entry point's main method,
 * covering boundary conditions not addressed by SpringBootMainMethodInvocationTest.
 * The Spring Boot plugin's bootRun task invokes main(); these tests verify
 * the delegation contract holds for uncommon argument patterns.
 */
class MainMethodEdgeCaseTest {

    @Test
    void main_shouldDelegateNullArgs_toSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    (String[]) isNull()
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(null);

            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    (String[]) isNull()
            ));
        }
    }

    @Test
    void main_shouldDelegateSingleArg_toSpringApplicationRun() {
        String[] args = {"--server.port=9090"};

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
    void main_shouldDelegateManyArgs_toSpringApplicationRun() {
        String[] args = {
                "--server.port=8080",
                "--spring.profiles.active=dev,test",
                "--debug",
                "--spring.datasource.url=jdbc:h2:mem:testdb"
        };

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
}
