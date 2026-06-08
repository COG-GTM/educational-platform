package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Validates that deep reflection access to private fields works on Java 26.
 * <p>
 * Hibernate/JPA relies on {@code setAccessible(true)} to read and write private
 * entity fields at runtime. Java's module system has progressively tightened
 * access controls (strong encapsulation in Java 16+). If Java 26 introduces
 * further restrictions, entity mapping would fail with
 * {@link InaccessibleObjectException}.
 * <p>
 * Unlike {@link DomainModelMetadataJava26Test} which verifies structural metadata
 * (types, modifiers), this test exercises the <em>write path</em>: instantiation
 * via no-arg constructor + reflective field assignment, exactly as Hibernate does
 * when materializing entities from the database.
 */
public class Java26DeepReflectionAccessTest {

    static Stream<Arguments> entityFieldsForDeepAccess() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.course.Course", "id"),
                Arguments.of("com.educational.platform.courses.course.Course", "uuid"),
                Arguments.of("com.educational.platform.administration.course.CourseProposal", "id"),
                Arguments.of("com.educational.platform.administration.course.CourseProposal", "uuid"),
                Arguments.of("com.educational.platform.course.enrollments.CourseEnrollment", "id"),
                Arguments.of("com.educational.platform.course.reviews.CourseReview", "id"),
                Arguments.of("com.educational.platform.users.User", "id")
        );
    }

    @ParameterizedTest(name = "setAccessible(true) should work on {0}.{1}")
    @MethodSource("entityFieldsForDeepAccess")
    void setAccessible_shouldSucceed_onPrivateEntityField(String className, String fieldName) throws Exception {
        Class<?> clazz = Class.forName(className);
        Field field = clazz.getDeclaredField(fieldName);

        assertThatCode(() -> field.setAccessible(true))
                .as("setAccessible(true) on %s.%s should not throw InaccessibleObjectException on Java 26",
                        clazz.getSimpleName(), fieldName)
                .doesNotThrowAnyException();

        // Verify trySetAccessible also works (preferred API in Java 9+)
        assertThat(field.trySetAccessible())
                .as("trySetAccessible() on %s.%s should return true",
                        clazz.getSimpleName(), fieldName)
                .isTrue();
    }

    @ParameterizedTest(name = "Reflective instantiation + field read should work: {0}")
    @MethodSource("entityClassesForInstantiation")
    void reflectiveInstantiation_shouldSucceed_onJava26Entity(String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        // JPA/Hibernate creates entities via no-arg constructor reflectively
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        assertThat(instance)
                .as("Reflective instantiation of %s via no-arg constructor should succeed on Java 26",
                        clazz.getSimpleName())
                .isNotNull();

        // Then reads field values via reflection (Hibernate's field access strategy)
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            assertThatCode(() -> field.get(instance))
                    .as("Reading field %s.%s via reflection should not throw",
                            clazz.getSimpleName(), field.getName())
                    .doesNotThrowAnyException();
        }
    }

    static Stream<Arguments> entityClassesForInstantiation() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.course.Course"),
                Arguments.of("com.educational.platform.administration.course.CourseProposal"),
                Arguments.of("com.educational.platform.course.enrollments.CourseEnrollment"),
                Arguments.of("com.educational.platform.course.reviews.CourseReview"),
                Arguments.of("com.educational.platform.users.User")
        );
    }

    @Test
    void reflectiveFieldWrite_shouldSucceed_onUuidField() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        var constructor = courseClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object course = constructor.newInstance();

        Field uuidField = courseClass.getDeclaredField("uuid");
        uuidField.setAccessible(true);

        UUID testUuid = UUID.randomUUID();
        uuidField.set(course, testUuid);

        Object readBack = uuidField.get(course);
        assertThat(readBack)
                .as("UUID written reflectively should be readable on Java 26")
                .isEqualTo(testUuid);
    }

    @Test
    void reflectiveFieldWrite_shouldSucceed_onIdField() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        var constructor = courseClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object course = constructor.newInstance();

        Field idField = courseClass.getDeclaredField("id");
        idField.setAccessible(true);

        // id is Integer type in this domain model
        idField.set(course, 42);

        Object readBack = idField.get(course);
        assertThat(readBack)
                .as("Integer id written reflectively should be readable on Java 26")
                .isEqualTo(42);
    }

    @Test
    void allDeclaredFields_shouldBeMadeAccessible_withoutException() throws Exception {
        String[] entityClasses = {
                "com.educational.platform.courses.course.Course",
                "com.educational.platform.administration.course.CourseProposal",
                "com.educational.platform.course.enrollments.CourseEnrollment",
                "com.educational.platform.course.reviews.CourseReview",
                "com.educational.platform.users.User"
        };

        for (String className : entityClasses) {
            Class<?> clazz = Class.forName(className);
            for (Field field : clazz.getDeclaredFields()) {
                assertThatNoException()
                        .as("setAccessible on %s.%s should not throw", clazz.getSimpleName(), field.getName())
                        .isThrownBy(() -> field.setAccessible(true));
            }
        }
    }
}
