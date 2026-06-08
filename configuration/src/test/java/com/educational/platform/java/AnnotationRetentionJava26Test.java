package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that runtime annotations (JPA, Spring, Jakarta) are properly
 * retained in Java 26 compiled bytecode and accessible via reflection.
 * <p>
 * JPA, Spring, and Hibernate rely on runtime annotation scanning to
 * discover entities, inject dependencies, and configure persistence.
 * A bytecode-level regression in annotation retention would cause silent
 * failures: entities not detected, fields not mapped, handlers not registered.
 * <p>
 * This test reads annotation metadata via reflection on domain classes
 * compiled with Java 26, confirming the JVM correctly preserves
 * {@code RetentionPolicy.RUNTIME} annotations in class file major version 70.
 */
public class AnnotationRetentionJava26Test {

    private static final String ENTITY_ANNOTATION = "jakarta.persistence.Entity";
    private static final String ID_ANNOTATION = "jakarta.persistence.Id";
    private static final String GENERATED_VALUE_ANNOTATION = "jakarta.persistence.GeneratedValue";
    private static final String ONE_TO_MANY_ANNOTATION = "jakarta.persistence.OneToMany";
    private static final String ENUMERATED_ANNOTATION = "jakarta.persistence.Enumerated";

    // --- JPA @Entity annotation retention ---

    static Stream<Arguments> jpaEntityClasses() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.course.Course"),
                Arguments.of("com.educational.platform.administration.course.CourseProposal"),
                Arguments.of("com.educational.platform.course.enrollments.CourseEnrollment"),
                Arguments.of("com.educational.platform.course.reviews.CourseReview"),
                Arguments.of("com.educational.platform.users.User")
        );
    }

    @ParameterizedTest(name = "@Entity annotation should be retained on: {0}")
    @MethodSource("jpaEntityClasses")
    void entityAnnotation_shouldBeRetained_onJava26CompiledClass(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Class<? extends Annotation> entityAnnotation = loadAnnotationClass(ENTITY_ANNOTATION);

        assertThat(clazz.isAnnotationPresent(entityAnnotation))
                .as("@Entity should be retained at runtime on %s (Java 26 bytecode)", className)
                .isTrue();
    }

    @ParameterizedTest(name = "@Id field should be present on: {0}")
    @MethodSource("jpaEntityClasses")
    void idAnnotation_shouldBeRetained_onEntityField(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Class<? extends Annotation> idAnnotation = loadAnnotationClass(ID_ANNOTATION);

        boolean hasIdField = Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(f -> f.isAnnotationPresent(idAnnotation));

        assertThat(hasIdField)
                .as("@Id annotation should be present on at least one field of %s", className)
                .isTrue();
    }

    @Test
    void generatedValueAnnotation_shouldBeRetained_onCourseIdField() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Class<? extends Annotation> gvAnnotation = loadAnnotationClass(GENERATED_VALUE_ANNOTATION);
        Field idField = courseClass.getDeclaredField("id");

        assertThat(idField.isAnnotationPresent(gvAnnotation))
                .as("@GeneratedValue should be retained on Course.id field")
                .isTrue();

        Annotation gv = idField.getAnnotation(gvAnnotation);
        Object strategy = gv.annotationType().getMethod("strategy").invoke(gv);
        assertThat(strategy.toString())
                .as("GenerationType should be IDENTITY, accessible via annotation reflection")
                .isEqualTo("IDENTITY");
    }

    @Test
    void oneToManyAnnotation_shouldBeRetained_onCourse() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Class<? extends Annotation> otmAnnotation = loadAnnotationClass(ONE_TO_MANY_ANNOTATION);
        Field curriculumItemsField = courseClass.getDeclaredField("curriculumItems");

        assertThat(curriculumItemsField.isAnnotationPresent(otmAnnotation))
                .as("@OneToMany should be retained on Course.curriculumItems")
                .isTrue();

        Annotation otm = curriculumItemsField.getAnnotation(otmAnnotation);
        Object fetchType = otm.annotationType().getMethod("fetch").invoke(otm);
        assertThat(fetchType.toString())
                .as("FetchType should be accessible via annotation reflection")
                .isEqualTo("EAGER");
    }

    @Test
    void enumeratedAnnotation_shouldBeRetained_onCourseStatusFields() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Class<? extends Annotation> enumAnnotation = loadAnnotationClass(ENUMERATED_ANNOTATION);

        Field publishStatusField = courseClass.getDeclaredField("publishStatus");
        assertThat(publishStatusField.isAnnotationPresent(enumAnnotation))
                .as("@Enumerated should be retained on Course.publishStatus")
                .isTrue();

        Annotation enumerated = publishStatusField.getAnnotation(enumAnnotation);
        Object enumType = enumerated.annotationType().getMethod("value").invoke(enumerated);
        assertThat(enumType.toString())
                .as("EnumType should be STRING for publishStatus")
                .isEqualTo("STRING");
    }

    // --- Spring annotation retention ---

    static Stream<Arguments> springComponentClasses() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.course.create.CreateCourseCommandHandler",
                        "org.springframework.stereotype.Component"),
                Arguments.of("com.educational.platform.courses.course.CourseFactory",
                        "org.springframework.stereotype.Component"),
                Arguments.of("com.educational.platform.courses.CourseController",
                        "org.springframework.web.bind.annotation.RestController")
        );
    }

    @ParameterizedTest(name = "Spring annotation should be retained on: {0}")
    @MethodSource("springComponentClasses")
    void springAnnotation_shouldBeRetained_onJava26CompiledClass(String className, String annotationName) {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(className);
            Class<? extends Annotation> annotClass = loadAnnotationClass(annotationName);

            assertThat(clazz.isAnnotationPresent(annotClass))
                    .as("@%s should be retained at runtime on %s",
                            annotClass.getSimpleName(), className)
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    // --- Annotation metadata completeness ---

    @Test
    void entityAnnotations_shouldBeAccessible_asArray() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Annotation[] annotations = courseClass.getAnnotations();

        assertThat(annotations)
                .as("Course class should have at least one runtime annotation")
                .isNotEmpty();

        assertThat(Arrays.stream(annotations).map(a -> a.annotationType().getSimpleName()))
                .as("Annotation type names should include Entity")
                .contains("Entity");
    }

    @Test
    void fieldAnnotations_shouldSurvive_java26Compilation() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Field[] fields = courseClass.getDeclaredFields();

        long annotatedFieldCount = Arrays.stream(fields)
                .filter(f -> f.getAnnotations().length > 0)
                .count();

        assertThat(annotatedFieldCount)
                .as("Course should have multiple annotated fields after Java 26 compilation")
                .isGreaterThanOrEqualTo(4);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> loadAnnotationClass(String name) throws ClassNotFoundException {
        return (Class<? extends Annotation>) Class.forName(name);
    }
}
