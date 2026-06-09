package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Edge-case tests verifying exception propagation and unusual argument patterns
 * for the application main method. The Spring Boot plugin's bootRun task invokes
 * main() directly; these tests ensure failures surface correctly rather than
 * being silently swallowed.
 */
class MainMethodExceptionPropagationTest {

    @Test
    void main_shouldPropagateRuntimeException_fromSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            RuntimeException expected = new RuntimeException("Context initialization failed");

            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenThrow(expected);

            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Context initialization failed");
        }
    }

    @Test
    void main_shouldPropagateIllegalStateException_fromSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            IllegalStateException expected = new IllegalStateException("Port already in use");

            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenThrow(expected);

            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Port already in use");
        }
    }

    @Test
    void main_withEmptyStringArgs_shouldDelegateToSpringApplicationRun() {
        String[] args = {"", ""};

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
    void main_withJdbcUrlArg_shouldDelegateToSpringApplicationRun() {
        String[] args = {"--spring.datasource.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"};

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
