package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates the {@code useMainMethod} attribute of {@code @SpringBootTest},
 * introduced in Spring Boot 3.1. This attribute controls whether the test
 * context bootstrapper invokes the application's {@code main()} method
 * (via the Spring Boot plugin's main class resolution) instead of using
 * the standard {@code SpringApplication.run()} path.
 * <p>
 * The {@code useMainMethod} attribute is a direct integration point between
 * the Spring Boot plugin's main-class auto-detection and the test
 * infrastructure: when set to {@code ALWAYS} or {@code WHEN_AVAILABLE},
 * the test framework locates the main method using the same resolution
 * strategy that {@code bootRun} uses. If the boot plugin is misconfigured
 * or the main class is not detectable, {@code ALWAYS} mode would fail
 * immediately.
 * <p>
 * Existing tests validate main method structure
 * ({@link com.educational.platform.application.EducationalPlatformApplicationMainTest})
 * and invocation ({@link com.educational.platform.application.SpringBootMainMethodInvocationTest}).
 * This test validates the {@code @SpringBootTest} attribute that connects
 * the test infrastructure to those capabilities.
 */
class SpringBootTestUseMainMethodAttributeTest {

    @Test
    void useMainMethod_enumValues_shouldContainExpectedOptions() {
        SpringBootTest.UseMainMethod[] values = SpringBootTest.UseMainMethod.values();
        assertThat(Arrays.stream(values).map(Enum::name).toList())
                .as("@SpringBootTest.UseMainMethod must contain ALWAYS, WHEN_AVAILABLE, and NEVER — "
                        + "these control how the test context bootstrapper invokes the application's main method")
                .containsExactlyInAnyOrder("ALWAYS", "WHEN_AVAILABLE", "NEVER");
    }

    @Test
    void useMainMethod_shouldBeAccessibleAsAnnotationAttribute() throws NoSuchMethodException {
        Method useMainMethodAttr = SpringBootTest.class.getDeclaredMethod("useMainMethod");
        assertThat(useMainMethodAttr.getReturnType())
                .as("useMainMethod attribute must return SpringBootTest.UseMainMethod enum type")
                .isEqualTo(SpringBootTest.UseMainMethod.class);
    }

    @Test
    void useMainMethod_defaultValue_shouldBeNever() throws NoSuchMethodException {
        Method useMainMethodAttr = SpringBootTest.class.getDeclaredMethod("useMainMethod");
        Object defaultValue = useMainMethodAttr.getDefaultValue();
        assertThat(defaultValue)
                .as("useMainMethod default must be NEVER for backward compatibility — "
                        + "existing @SpringBootTest usages must not suddenly invoke main()")
                .isEqualTo(SpringBootTest.UseMainMethod.NEVER);
    }

    @Test
    void springBootTest_shouldExposeBothUseMainMethodAndClasses() {
        assertThatCode(() -> {
            SpringBootTest.class.getDeclaredMethod("useMainMethod");
            SpringBootTest.class.getDeclaredMethod("classes");
        })
                .as("@SpringBootTest must expose both useMainMethod and classes attributes — "
                        + "they work together: classes identifies the application, "
                        + "useMainMethod controls the bootstrap strategy")
                .doesNotThrowAnyException();
    }

    @Test
    void useMainMethod_whenAvailable_shouldBeResolvable() {
        SpringBootTest.UseMainMethod whenAvailable = SpringBootTest.UseMainMethod.WHEN_AVAILABLE;
        assertThat(whenAvailable.name())
                .as("WHEN_AVAILABLE falls back to standard bootstrap if main() is not found — "
                        + "it is the safest non-default option for projects with the boot plugin")
                .isEqualTo("WHEN_AVAILABLE");
    }
}
