package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the class-level contract of the application entry point that
 * is not covered by EducationalPlatformApplicationMainTest or
 * SpringApplicationRunContractTest. Guards against accidental interface
 * implementations or structural changes that could affect Spring proxying
 * or the Spring Boot plugin's main class resolution.
 */
class ApplicationClassInterfaceContractTest {

    @Test
    void applicationClass_shouldNotImplementAnyInterfaces() {
        assertThat(EducationalPlatformApplication.class.getInterfaces())
                .as("Application class must be a plain POJO with no interfaces — "
                        + "implementing an interface could cause unwanted JDK proxy creation "
                        + "and conflict with CGLIB proxying required by @Configuration")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotBeSerializable() {
        assertThat(Serializable.class.isAssignableFrom(EducationalPlatformApplication.class))
                .as("Application class must not be Serializable — "
                        + "it is a singleton context root, not a transferable object")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotDeclareAnyFields() {
        assertThat(EducationalPlatformApplication.class.getDeclaredFields())
                .as("Application class should have no instance or static fields — "
                        + "state belongs in Spring-managed beans, not the entry point")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldHaveExactlyOnePublicMethod() {
        long publicMethodCount = java.util.Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .count();
        assertThat(publicMethodCount)
                .as("Application class should declare exactly one public method (main) — "
                        + "business logic belongs in domain classes, not the entry point")
                .isEqualTo(1);
    }

    @Test
    void applicationClass_shouldNotDeclareGenericTypeParameters() {
        assertThat(EducationalPlatformApplication.class.getTypeParameters())
                .as("Application class must not have generic type parameters")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotBeAnnotatedWithDeprecated() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(Deprecated.class))
                .as("Application class must not be @Deprecated — it is the active entry point")
                .isFalse();
    }
}
