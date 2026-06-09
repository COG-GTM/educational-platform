package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the inheritance and access-level contract of the application
 * entry point class. The Spring Boot plugin's main-class auto-detection
 * requires the application class to be a concrete, non-final, top-level
 * public class with no superclass other than Object. CGLIB proxying
 * (used by {@code @SpringBootApplication}'s implicit {@code @Configuration})
 * further requires the class to be non-final and not extend a final class.
 * <p>
 * Complements {@link ApplicationClassInterfaceContractTest} (no interfaces,
 * no fields), {@link ApplicationClassStaticStateGuardTest} (no static state,
 * no inner classes), and {@link MainMethodDeclarationContractTest}
 * (main method contract including superclass = Object).
 * This test consolidates inheritance-specific assertions not covered by
 * the other contract tests.
 */
class ApplicationClassInheritanceContractTest {

    @Test
    void applicationClass_shouldNotExtendAnyFrameworkBaseClass() {
        Class<?> superclass = EducationalPlatformApplication.class.getSuperclass();
        assertThat(superclass)
                .as("Application class must extend Object directly — "
                        + "extending a framework class (e.g., SpringBootServletInitializer) "
                        + "would change the boot entry-point contract and affect how "
                        + "the Spring Boot plugin resolves the main class")
                .isEqualTo(Object.class);
        assertThat(superclass.getName())
                .as("Superclass must be java.lang.Object")
                .isEqualTo("java.lang.Object");
    }

    @Test
    void applicationClass_shouldNotBeAnnotatedWithInherited() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                java.lang.annotation.Inherited.class))
                .as("Application class must NOT carry @Inherited — "
                        + "inherited annotations on a composition root would propagate "
                        + "to any accidental subclass, causing unexpected proxying behavior")
                .isFalse();
    }

    @Test
    void applicationClass_shouldBeATopLevelConcreteClass() {
        int modifiers = EducationalPlatformApplication.class.getModifiers();
        assertThat(Modifier.isPublic(modifiers)).isTrue();
        assertThat(Modifier.isAbstract(modifiers)).isFalse();
        assertThat(Modifier.isFinal(modifiers)).isFalse();
        assertThat(EducationalPlatformApplication.class.isMemberClass())
                .as("Application class must NOT be a member (inner) class")
                .isFalse();
        assertThat(EducationalPlatformApplication.class.isLocalClass())
                .as("Application class must NOT be a local class")
                .isFalse();
        assertThat(EducationalPlatformApplication.class.isAnonymousClass())
                .as("Application class must NOT be anonymous")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotImplementCloseable() {
        assertThat(AutoCloseable.class.isAssignableFrom(EducationalPlatformApplication.class))
                .as("Application class must NOT implement AutoCloseable/Closeable — "
                        + "lifecycle management is handled by the Spring context, "
                        + "not the entry point class")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotImplementApplicationContextAware() {
        boolean implementsAware = java.util.Arrays.stream(
                        EducationalPlatformApplication.class.getInterfaces())
                .anyMatch(i -> i.getSimpleName().contains("Aware"));
        assertThat(implementsAware)
                .as("Application class must NOT implement any *Aware interfaces — "
                        + "the composition root should not hold references to the application "
                        + "context or other infrastructure objects")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotBeAbstractAndPublic() {
        assertThat(Modifier.isPublic(EducationalPlatformApplication.class.getModifiers())
                && !Modifier.isAbstract(EducationalPlatformApplication.class.getModifiers()))
                .as("Application class must be public and non-abstract for Spring Boot "
                        + "plugin to instantiate and proxy it")
                .isTrue();
    }
}
