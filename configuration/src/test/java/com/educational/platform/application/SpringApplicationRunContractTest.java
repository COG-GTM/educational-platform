package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Validates the contract between the application entry point and
 * SpringApplication.run(). The Spring Boot plugin's bootRun task invokes
 * main() which must delegate exactly once to SpringApplication.run with
 * the correct class reference. These tests cover contract aspects not
 * addressed by SpringBootMainMethodInvocationTest (which focuses on
 * argument passing and basic delegation).
 */
class SpringApplicationRunContractTest {

    @Test
    void main_shouldDelegateExactlyOnce_toSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(new String[]{});

            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            ), times(1));
        }
    }

    @Test
    void main_shouldPassSameArgsArrayReference_toSpringApplicationRun() {
        String[] originalArgs = {"--server.port=8080"};

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(originalArgs);

            springApp.verify(() -> SpringApplication.run(
                    EducationalPlatformApplication.class,
                    originalArgs
            ));
        }
    }

    @Test
    void main_shouldUseExactApplicationClass_notSubclassOrSuperclass() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(new String[]{});

            // Verify the exact class reference is passed (not Object.class, etc.)
            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            ));
            springApp.verifyNoMoreInteractions();
        }
    }

    @Test
    void main_shouldNotCallOtherSpringApplicationMethods() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(new String[]{});

            // Verify the expected call happened exactly once
            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            ), times(1));
            // Then verify nothing else was called
            springApp.verifyNoMoreInteractions();
        }
    }

    @Test
    void applicationClass_shouldNotExtendSpringApplication() {
        assertThat(SpringApplication.class.isAssignableFrom(EducationalPlatformApplication.class))
                .as("Application class must not extend SpringApplication — "
                        + "it should delegate via SpringApplication.run()")
                .isFalse();
    }

    @Test
    void applicationClass_superclass_shouldBeObject() {
        assertThat(EducationalPlatformApplication.class.getSuperclass())
                .as("Application class must be a plain POJO extending Object, "
                        + "not a framework class that might conflict with the plugin")
                .isEqualTo(Object.class);
    }
}
