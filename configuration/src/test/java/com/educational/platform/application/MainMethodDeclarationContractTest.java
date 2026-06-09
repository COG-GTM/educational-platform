package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates main method declaration details not covered by
 * {@link EducationalPlatformApplicationMainTest} (signature, modifiers, return type)
 * or {@link MainMethodEdgeCaseTest} (argument delegation). These tests guard
 * the JVM entry-point contract that the Spring Boot plugin's bootRun task
 * relies on: no checked exceptions, no overloads, and no synthetic/bridge methods
 * that could confuse the plugin's main-class resolution.
 */
class MainMethodDeclarationContractTest {

    @Test
    void mainMethod_shouldNotDeclareCheckedExceptions() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(main.getExceptionTypes())
                .as("main() must not declare checked exceptions — the JVM entry point "
                        + "contract requires a clean void main(String[]) signature; "
                        + "declaring throws would signal a broken contract to bootRun")
                .isEmpty();
    }

    @Test
    void mainMethod_shouldNotHaveOverloads() {
        List<Method> mainMethods = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("main"))
                .toList();
        assertThat(mainMethods)
                .as("Only one main() method should exist — overloads would confuse "
                        + "the Spring Boot plugin's main-class detection; the plugin "
                        + "expects exactly one public static void main(String[])")
                .hasSize(1);
    }

    @Test
    void mainMethod_shouldNotBeSynchronized() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(Modifier.isSynchronized(main.getModifiers()))
                .as("main() must not be synchronized — bootRun invokes it from a "
                        + "single thread; synchronized would add unnecessary contention "
                        + "and a misleading concurrency signal")
                .isFalse();
    }

    @Test
    void mainMethod_shouldNotBeFinal() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(Modifier.isFinal(main.getModifiers()))
                .as("main() must not be final — although static methods cannot be "
                        + "overridden, the final modifier is superfluous and signals "
                        + "a non-standard declaration")
                .isFalse();
    }

    @Test
    void mainMethod_shouldNotHaveAnnotations() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(main.getDeclaredAnnotations())
                .as("main() must not carry any annotations — annotations on the "
                        + "entry-point method could interfere with Spring Boot plugin "
                        + "resolution or cause unexpected AOP proxying")
                .isEmpty();
    }

    @Test
    void mainMethod_shouldNotBeNative() throws NoSuchMethodException {
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);
        assertThat(Modifier.isNative(main.getModifiers()))
                .as("main() must not be native — the Spring Boot plugin requires "
                        + "a standard Java implementation for bootRun")
                .isFalse();
    }

    @Test
    void applicationClass_shouldExtendObjectDirectly() {
        assertThat(EducationalPlatformApplication.class.getSuperclass())
                .as("Application class must extend Object directly — extending a "
                        + "framework base class could interfere with CGLIB proxying "
                        + "and main-class auto-detection by the Spring Boot plugin")
                .isEqualTo(Object.class);
    }
}
