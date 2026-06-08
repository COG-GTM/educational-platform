package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying the structural requirements of the application entry point
 * for the Spring Boot plugin (bootRun, bootJar) to function correctly.
 * These tests do NOT load a Spring context; they validate class-level contracts.
 */
class EducationalPlatformApplicationMainTest {

    @Test
    void mainMethod_shouldExist_withCorrectSignature() throws NoSuchMethodException {
        // given
        Method main = EducationalPlatformApplication.class.getDeclaredMethod("main", String[].class);

        // then
        assertThat(Modifier.isPublic(main.getModifiers()))
                .as("main method must be public for bootRun")
                .isTrue();
        assertThat(Modifier.isStatic(main.getModifiers()))
                .as("main method must be static for bootRun")
                .isTrue();
        assertThat(main.getReturnType())
                .as("main method must return void")
                .isEqualTo(Void.TYPE);
    }

    @Test
    void applicationClass_shouldBePublic() {
        // The Spring Boot plugin requires the application class to be public
        assertThat(Modifier.isPublic(EducationalPlatformApplication.class.getModifiers()))
                .as("Application class must be public for Spring Boot plugin")
                .isTrue();
    }

    @Test
    void applicationClass_shouldNotBeAbstract() {
        assertThat(Modifier.isAbstract(EducationalPlatformApplication.class.getModifiers()))
                .as("Application class must not be abstract to be instantiable by Spring")
                .isFalse();
    }

    @Test
    void applicationClass_shouldResideInBasePackage() {
        // The application class must be in a base package for component scanning
        // to discover all modules under com.educational.platform.*
        String packageName = EducationalPlatformApplication.class.getPackageName();
        assertThat(packageName)
                .as("Application class must be in base package for component scanning to find all modules")
                .isEqualTo("com.educational.platform");
    }

    @Test
    void applicationClass_shouldHaveNoArgConstructor() {
        // Spring requires a no-arg constructor to instantiate the application class
        assertThat(EducationalPlatformApplication.class.getConstructors())
                .as("Application class must have at least one public constructor")
                .hasSizeGreaterThan(0);

        boolean hasNoArg = false;
        for (var ctor : EducationalPlatformApplication.class.getConstructors()) {
            if (ctor.getParameterCount() == 0) {
                hasNoArg = true;
                break;
            }
        }
        assertThat(hasNoArg)
                .as("Application class must have a no-arg constructor for Spring instantiation")
                .isTrue();
    }
}
