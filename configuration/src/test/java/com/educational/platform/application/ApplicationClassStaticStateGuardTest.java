package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against static state, inner classes, and initializer blocks in the
 * application entry point. The Spring Boot plugin's bootRun task launches
 * the application via the main method; any static state in the application
 * class could cause ordering issues with the Spring context lifecycle or
 * create classloader isolation problems when the boot loader wraps the
 * application JAR.
 * <p>
 * Complements {@link ApplicationClassInterfaceContractTest} (no fields,
 * no interfaces), {@link ApplicationClassNoBeanDeclarationTest} (no @Bean
 * methods), and {@link MainMethodDeclarationContractTest} (main method
 * structural contract).
 */
class ApplicationClassStaticStateGuardTest {

    @Test
    void applicationClass_shouldNotDeclareStaticFields() {
        List<Field> staticFields = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .toList();
        assertThat(staticFields)
                .as("Application class must NOT have static fields — "
                        + "static state in the entry point can cause classloader issues "
                        + "with the Spring Boot loader and interfere with context lifecycle")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotDeclareInnerClasses() {
        assertThat(EducationalPlatformApplication.class.getDeclaredClasses())
                .as("Application class must NOT declare inner or nested classes — "
                        + "the entry point must be a minimal composition root; "
                        + "inner classes would be scanned by @SpringBootApplication "
                        + "and could introduce unintended beans")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotBeAnEnum() {
        assertThat(EducationalPlatformApplication.class.isEnum())
                .as("Application class must NOT be an enum — "
                        + "the Spring Boot plugin requires a standard class with main()")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotBeAnInterface() {
        assertThat(EducationalPlatformApplication.class.isInterface())
                .as("Application class must NOT be an interface — "
                        + "the Spring Boot plugin requires an instantiable class")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotBeARecord() {
        assertThat(EducationalPlatformApplication.class.isRecord())
                .as("Application class must NOT be a record — "
                        + "records are immutable data carriers, not suitable as "
                        + "Spring Boot entry points requiring CGLIB proxying")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotBeSealed() {
        assertThat(EducationalPlatformApplication.class.isSealed())
                .as("Application class must NOT be sealed — "
                        + "sealed classes restrict subclassing, which would prevent "
                        + "CGLIB proxy generation required by @SpringBootApplication")
                .isFalse();
    }

    @Test
    void applicationClass_shouldHaveExactlyOneConstructor() {
        assertThat(EducationalPlatformApplication.class.getDeclaredConstructors())
                .as("Application class must have exactly one constructor (the default no-arg) — "
                        + "additional constructors suggest state or dependency injection "
                        + "that belongs in Spring-managed beans, not the entry point")
                .hasSize(1);
        assertThat(EducationalPlatformApplication.class.getDeclaredConstructors()[0].getParameterCount())
                .as("The single constructor must be no-arg")
                .isEqualTo(0);
    }
}
