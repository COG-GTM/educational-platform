package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Validates that the application main method is stateless and supports
 * repeated invocations. While bootRun typically invokes main() once,
 * test tooling or restart scenarios may invoke it multiple times.
 * Each invocation must delegate independently to SpringApplication.run
 * without accumulated state or side effects.
 */
class MainMethodConcurrentInvocationTest {

    @Test
    void main_shouldDelegateOnEveryInvocation_withoutAccumulatingState() {
        AtomicInteger invocationCount = new AtomicInteger(0);

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenAnswer(invocation -> {
                invocationCount.incrementAndGet();
                return mock(ConfigurableApplicationContext.class);
            });

            EducationalPlatformApplication.main(new String[]{});
            EducationalPlatformApplication.main(new String[]{"--server.port=9090"});
            EducationalPlatformApplication.main(new String[]{});

            assertThat(invocationCount.get())
                    .as("Each main() call must independently delegate to SpringApplication.run")
                    .isEqualTo(3);

            springApp.verify(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            ), times(3));
        }
    }

    @Test
    void main_shouldNotRetainStateFromPreviousInvocation() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            // First invocation with specific args
            String[] args1 = {"--spring.profiles.active=dev"};
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    eq(args1)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args1);

            springApp.verify(() -> SpringApplication.run(
                    EducationalPlatformApplication.class,
                    args1
            ));

            // Second invocation with different args must pass those args, not the first ones
            String[] args2 = {"--spring.profiles.active=prod"};
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    eq(args2)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args2);

            springApp.verify(() -> SpringApplication.run(
                    EducationalPlatformApplication.class,
                    args2
            ));
        }
    }

    @Test
    void main_shouldPropagateExceptionOnSecondInvocation_afterSuccessfulFirst() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class))
                    .thenThrow(new RuntimeException("Second invocation failure"));

            // First invocation succeeds
            EducationalPlatformApplication.main(new String[]{});

            // Second invocation must propagate the exception independently
            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Second invocation failure");
        }
    }
}
