package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the Spring Boot application entry point is correctly structured
 * and discoverable on Java 26.
 * <p>
 * Spring Boot's auto-configuration relies on the {@code @SpringBootApplication}
 * annotation being present on the main class, which must also have a
 * {@code public static void main(String[])} method. The annotation scanner uses
 * ASM to discover these at compile time and reflection at runtime. If Java 26
 * bytecode changes affect how Spring Boot discovers its entry point, the
 * application would fail to start.
 * <p>
 * {@link SpringBootDependencyCompatibilityTest} verifies classpath availability.
 * {@link SpringComponentScanningJava26Test} verifies component scanning.
 * This test verifies the <em>application entry point</em> structure.
 */
public class SpringApplicationEntryPointJava26Test {

    private static final String APPLICATION_CLASS = "com.educational.platform.EducationalPlatformApplication";

    @Test
    void applicationClass_shouldBeLoadable() {
        assertThatCode(() -> Class.forName(APPLICATION_CLASS))
                .as("Spring Boot application class should be loadable on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void applicationClass_shouldHave_springBootApplicationAnnotation() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);
        Class<? extends Annotation> sbaAnnotation =
                loadAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication");

        assertThat(appClass.isAnnotationPresent(sbaAnnotation))
                .as("Application class should be annotated with @SpringBootApplication")
                .isTrue();
    }

    @Test
    void applicationClass_shouldHave_publicStaticMainMethod() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);
        Method mainMethod = appClass.getDeclaredMethod("main", String[].class);

        assertThat(Modifier.isPublic(mainMethod.getModifiers()))
                .as("main() should be public")
                .isTrue();

        assertThat(Modifier.isStatic(mainMethod.getModifiers()))
                .as("main() should be static")
                .isTrue();

        assertThat(mainMethod.getReturnType())
                .as("main() should return void")
                .isEqualTo(void.class);
    }

    @Test
    void applicationClass_shouldBePublic() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);

        assertThat(Modifier.isPublic(appClass.getModifiers()))
                .as("Application class should be public for Spring Boot startup")
                .isTrue();
    }

    @Test
    void applicationClass_shouldNotBeAbstract() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);

        assertThat(Modifier.isAbstract(appClass.getModifiers()))
                .as("Application class should not be abstract")
                .isFalse();
    }

    @Test
    void applicationClass_shouldReside_inPlatformRootPackage() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);

        assertThat(appClass.getPackageName())
                .as("Application class should be in the platform root package")
                .isEqualTo("com.educational.platform");
    }

    @ParameterizedTest(name = "Meta-annotation should be resolvable on @SpringBootApplication: {0}")
    @ValueSource(strings = {
            "org.springframework.boot.SpringBootConfiguration",
            "org.springframework.boot.autoconfigure.EnableAutoConfiguration"
    })
    void springBootApplication_shouldCarry_metaAnnotations(String metaAnnotationName) throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);
        Class<? extends Annotation> sbaAnnotation =
                loadAnnotation("org.springframework.boot.autoconfigure.SpringBootApplication");
        Class<? extends Annotation> metaAnnotation = loadAnnotation(metaAnnotationName);

        // @SpringBootApplication is annotated with @SpringBootConfiguration and @EnableAutoConfiguration
        Annotation sba = appClass.getAnnotation(sbaAnnotation);
        assertThat(sba).as("@SpringBootApplication should be present").isNotNull();

        // The meta-annotations should be on @SpringBootApplication's annotation type
        boolean hasOnSBA = sba.annotationType().isAnnotationPresent(metaAnnotation);

        assertThat(hasOnSBA)
                .as("@SpringBootApplication should carry @%s as a meta-annotation on Java 26",
                        metaAnnotation.getSimpleName())
                .isTrue();
    }

    @Test
    void componentScanBasePackage_shouldBeInferrable() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);

        // Spring Boot uses the application class's package as the base for component scanning
        String basePackage = appClass.getPackageName();
        assertThat(basePackage)
                .as("Base package for component scanning should be resolvable")
                .isNotBlank()
                .startsWith("com.educational.platform");
    }

    @Test
    void applicationClass_annotations_shouldBeAccessible_asArray() throws Exception {
        Class<?> appClass = Class.forName(APPLICATION_CLASS);
        Annotation[] annotations = appClass.getAnnotations();

        assertThat(annotations)
                .as("Application class annotations should be accessible on Java 26")
                .isNotEmpty();

        assertThat(Arrays.stream(annotations)
                .map(a -> a.annotationType().getSimpleName()))
                .as("Should include SpringBootApplication")
                .contains("SpringBootApplication");
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadAnnotation(String name) throws ClassNotFoundException {
        return (Class<? extends Annotation>) Class.forName(name);
    }
}
