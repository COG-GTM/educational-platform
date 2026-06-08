package com.educational.platform.java;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that Spring's component scanning, dependency injection, and
 * event-driven infrastructure annotations are correctly discoverable on
 * Java 26 compiled classes.
 * <p>
 * Spring Boot relies on classpath scanning (via ASM) and reflection to
 * discover {@code @Component}, {@code @EventListener}, {@code @Async},
 * and constructor-injection candidates. If Java 26 bytecode changes
 * break ASM-based scanning or reflection-based annotation discovery,
 * the entire application context would fail to start.
 * <p>
 * Existing tests verify annotation retention ({@link AnnotationRetentionJava26Test})
 * and class loading ({@link SpringBootDependencyCompatibilityTest}). This test
 * covers the <em>scanning and discovery</em> code paths: constructor parameter
 * resolution, event listener method detection, and component hierarchy traversal.
 */
public class SpringComponentScanningJava26Test {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.educational.platform");

    // --- Constructor-based DI discovery on Java 26 ---

    @ParameterizedTest(name = "Constructor injection candidate should be discoverable: {0}")
    @ValueSource(strings = {
            "com.educational.platform.courses.course.CourseFactory",
            "com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler",
            "com.educational.platform.courses.course.create.CreateCourseCommandHandler"
    })
    void constructorInjection_shouldBeDiscoverable_onJava26Class(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        assertThat(constructors)
                .as("Spring DI requires at least one constructor for '%s'", className)
                .isNotEmpty();

        Constructor<?> primaryConstructor = constructors[0];
        Class<?>[] paramTypes = primaryConstructor.getParameterTypes();

        assertThat(paramTypes.length)
                .as("Primary constructor of '%s' should have injectable parameters", className)
                .isGreaterThan(0);

        for (Class<?> paramType : paramTypes) {
            assertThat(paramType.getName())
                    .as("Constructor param type should be resolvable")
                    .isNotNull();
        }
    }

    // --- @EventListener method discovery on Java 26 ---

    @Test
    void eventListenerMethods_shouldBeDetectable_viaReflection() throws Exception {
        Class<?> handlerClass = Class.forName(
                "com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler");

        boolean hasEventListener = Arrays.stream(handlerClass.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(
                        loadAnnotation("org.springframework.context.event.EventListener")));

        assertThat(hasEventListener)
                .as("@EventListener method should be discoverable on Java 26 compiled handler")
                .isTrue();
    }

    @Test
    void asyncAnnotation_shouldBeRetained_onEventHandlerMethods() throws Exception {
        Class<?> handlerClass = Class.forName(
                "com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler");

        boolean hasAsync = Arrays.stream(handlerClass.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(
                        loadAnnotation("org.springframework.scheduling.annotation.Async")));

        assertThat(hasAsync)
                .as("@Async annotation should be retained on event handler methods (Java 26)")
                .isTrue();
    }

    @Test
    void eventListenerMethod_parameterType_shouldBeResolvable() throws Exception {
        Class<?> handlerClass = Class.forName(
                "com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler");
        Class<? extends Annotation> eventListenerAnnotation =
                loadAnnotation("org.springframework.context.event.EventListener");

        Method listenerMethod = Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(eventListenerAnnotation))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No @EventListener method found"));

        Class<?>[] paramTypes = listenerMethod.getParameterTypes();
        assertThat(paramTypes).hasSize(1);
        assertThat(paramTypes[0].getName())
                .as("Event listener parameter type should be the integration event class")
                .contains("IntegrationEvent");
    }

    // --- ArchUnit component scanning on Java 26 ---

    @Test
    void archUnit_shouldDetect_componentAnnotatedClasses_acrossAllModules() {
        long componentCount = PRODUCTION_CLASSES.stream()
                .filter(c -> c.isAnnotatedWith("org.springframework.stereotype.Component"))
                .count();

        assertThat(componentCount)
                .as("ArchUnit should detect @Component classes across all bounded contexts")
                .isGreaterThan(5);
    }

    @Test
    void archUnit_shouldDetect_eventListenerMethods_onJava26Classes() {
        long eventListenerMethodCount = PRODUCTION_CLASSES.stream()
                .flatMap(c -> c.getMethods().stream())
                .filter(m -> m.isAnnotatedWith("org.springframework.context.event.EventListener"))
                .count();

        assertThat(eventListenerMethodCount)
                .as("ArchUnit should detect @EventListener methods on Java 26 classes")
                .isGreaterThan(0);
    }

    // --- Component hierarchy traversal ---

    @Test
    void restController_metaAnnotation_shouldResolve_toComponent() throws Exception {
        Class<?> controllerClass = Class.forName("com.educational.platform.courses.CourseController");
        Class<? extends Annotation> restController =
                loadAnnotation("org.springframework.web.bind.annotation.RestController");

        assertThat(controllerClass.isAnnotationPresent(restController)).isTrue();

        // @RestController is meta-annotated with @Controller, which is
        // meta-annotated with @Component. Verify the annotation chain is intact.
        Annotation rcAnnotation = controllerClass.getAnnotation(restController);
        Annotation[] metaAnnotations = rcAnnotation.annotationType().getAnnotations();

        boolean hasControllerMeta = Arrays.stream(metaAnnotations)
                .anyMatch(a -> a.annotationType().getSimpleName().equals("Controller"));

        assertThat(hasControllerMeta)
                .as("@RestController's @Controller meta-annotation should be resolvable on Java 26")
                .isTrue();
    }

    @Test
    void componentAnnotation_value_shouldBeAccessible() throws Exception {
        Class<?> factoryClass = Class.forName("com.educational.platform.courses.course.CourseFactory");
        Class<? extends Annotation> componentAnnotation =
                loadAnnotation("org.springframework.stereotype.Component");

        Annotation component = factoryClass.getAnnotation(componentAnnotation);
        assertThat(component)
                .as("CourseFactory should have @Component annotation accessible via reflection")
                .isNotNull();

        // Verify the annotation's value() method is callable
        Method valueMethod = component.annotationType().getMethod("value");
        Object value = valueMethod.invoke(component);
        assertThat(value).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadAnnotation(String name) {
        try {
            return (Class<? extends Annotation>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Annotation class not found: " + name, e);
        }
    }
}
