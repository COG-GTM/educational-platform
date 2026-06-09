package com.educational.platform.application;

import com.educational.platform.EducationalPlatformApplication;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against declaring @Bean methods in the application entry point.
 * The application class is the composition root annotated with
 * {@code @SpringBootApplication}; it should only contain the main() method.
 * Bean definitions belong in dedicated @Configuration classes within their
 * respective bounded-context modules. Adding @Bean methods to the entry point:
 * <ul>
 *   <li>Violates modular monolith boundaries by centralizing bean creation</li>
 *   <li>Makes the boot plugin's main class detection potentially fragile</li>
 *   <li>Creates tight coupling between the composition root and domain beans</li>
 * </ul>
 * Complements {@link ApplicationClassInterfaceContractTest} which validates
 * structural properties and {@link SpringBootApplicationProxyBeanMethodsTest}
 * which validates the proxyBeanMethods attribute.
 */
class ApplicationClassNoBeanDeclarationTest {

    @Test
    void applicationClass_shouldNotDeclareAnyBeanMethods() {
        List<Method> beanMethods = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Bean.class))
                .toList();
        assertThat(beanMethods)
                .as("Application class must NOT declare any @Bean methods — "
                        + "bean definitions belong in module-specific @Configuration classes, "
                        + "not the composition root")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotDeclareConfigurationMethods() {
        List<Method> allMethods = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredMethods())
                .filter(m -> !m.getName().equals("main"))
                .toList();
        assertThat(allMethods)
                .as("Application class must only declare main() — "
                        + "no factory, helper, or configuration methods should exist "
                        + "in the boot entry point")
                .isEmpty();
    }

    @Test
    void applicationClass_shouldNotHaveConfigurationAnnotation() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.context.annotation.Configuration.class))
                .as("@Configuration must NOT be explicitly declared on the application class — "
                        + "@SpringBootApplication already carries it transitively via "
                        + "@SpringBootConfiguration")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotDeclareConditionalAnnotations() {
        boolean hasConditional = Arrays.stream(
                        EducationalPlatformApplication.class.getDeclaredAnnotations())
                .anyMatch(a -> a.annotationType().getSimpleName().startsWith("Conditional"));
        assertThat(hasConditional)
                .as("Application class must NOT have @Conditional annotations — "
                        + "conditional logic belongs in auto-configuration or module configs")
                .isFalse();
    }

    @Test
    void applicationClass_shouldNotDeclareProfileAnnotation() {
        assertThat(EducationalPlatformApplication.class.isAnnotationPresent(
                org.springframework.context.annotation.Profile.class))
                .as("@Profile must NOT be on the application class — "
                        + "the entry point must be active in all profiles; "
                        + "profile-specific behavior belongs in module configurations")
                .isFalse();
    }
}
