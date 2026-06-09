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

/**
 * Validates the main method's behavior with null and boundary-condition
 * arguments that are not covered by {@link MainMethodEdgeCaseTest}
 * (which tests null, single-arg, and many-args delegation) or
 * {@link MainMethodExceptionPropagationTest} (which tests empty strings
 * and JDBC URL args). This test focuses on args array contents with
 * null elements and whitespace-only values — conditions that could
 * occur if bootRun is invoked programmatically with malformed arguments.
 */
class MainMethodNullSafetyTest {

    @Test
    void main_withArgsContainingNullElement_shouldDelegateWithoutFiltering() {
        String[] args = {"--server.port=8080", null, "--debug"};

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    eq(args)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args);

            springApp.verify(() -> SpringApplication.run(
                    EducationalPlatformApplication.class,
                    args
            ));
        }
    }

    @Test
    void main_withWhitespaceOnlyArgs_shouldDelegateUnmodified() {
        String[] args = {"  ", "\t", "\n"};

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    eq(args)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args);

            springApp.verify(() -> SpringApplication.run(
                    EducationalPlatformApplication.class,
                    args
            ));
        }
    }

    @Test
    void main_withLargeArgArray_shouldDelegateWithoutTruncation() {
        String[] args = new String[100];
        for (int i = 0; i < 100; i++) {
            args[i] = "--arg" + i + "=value" + i;
        }

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args);

            springApp.verify(() -> SpringApplication.run(
                    EducationalPlatformApplication.class,
                    args
            ));
        }
    }

    @Test
    void main_shouldNotModifyInputArgsArray() {
        String[] args = {"--spring.profiles.active=test", "--server.port=0"};
        String[] argsCopy = args.clone();

        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenReturn(mock(ConfigurableApplicationContext.class));

            EducationalPlatformApplication.main(args);

            assertThat(args)
                    .as("main method must not modify the input args array — "
                            + "the caller may rely on the array contents after invocation")
                    .containsExactly(argsCopy);
        }
    }
}
