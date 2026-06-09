package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

/**
 * Verifies that JVM-level Errors (not just Exceptions) propagate correctly
 * from SpringApplication.run through the main method. The Spring Boot plugin's
 * bootRun task invokes main() directly — if Errors are swallowed, critical
 * JVM failures (OutOfMemoryError, StackOverflowError) would go undetected.
 */
class MainMethodErrorPropagationTest {

    @Test
    void main_shouldPropagateOutOfMemoryError() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            OutOfMemoryError expected = new OutOfMemoryError("Heap space exhausted");

            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenThrow(expected);

            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(OutOfMemoryError.class)
                    .hasMessage("Heap space exhausted");
        }
    }

    @Test
    void main_shouldPropagateStackOverflowError() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            StackOverflowError expected = new StackOverflowError("Stack depth exceeded");

            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenThrow(expected);

            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(StackOverflowError.class)
                    .hasMessage("Stack depth exceeded");
        }
    }

    @Test
    void main_shouldPropagateExceptionInInitializerError() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            ExceptionInInitializerError expected = new ExceptionInInitializerError(
                    "Static initializer failed");

            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenThrow(expected);

            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(ExceptionInInitializerError.class);
        }
    }

    @Test
    void main_shouldPropagateNoClassDefFoundError() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            NoClassDefFoundError expected = new NoClassDefFoundError(
                    "com/missing/Dependency");

            springApp.when(() -> SpringApplication.run(
                    eq(EducationalPlatformApplication.class),
                    any(String[].class)
            )).thenThrow(expected);

            assertThatThrownBy(() -> EducationalPlatformApplication.main(new String[]{}))
                    .isInstanceOf(NoClassDefFoundError.class)
                    .hasMessage("com/missing/Dependency");
        }
    }
}
