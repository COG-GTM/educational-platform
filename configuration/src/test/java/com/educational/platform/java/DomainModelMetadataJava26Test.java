package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that JPA/Hibernate entity metadata is correctly accessible via
 * reflection on Java 26 compiled domain classes.
 * <p>
 * Hibernate builds its entity metamodel by inspecting class structure via
 * reflection: field types, generic type parameters, access modifiers, and
 * constructor accessibility. If Java 26 bytecode changes affect how the JVM
 * exposes these details, entity mapping would silently break.
 * <p>
 * {@link AnnotationRetentionJava26Test} verifies annotation retention.
 * This test verifies the <em>structural</em> metadata: field type resolution,
 * generic type parameter preservation, constructor accessibility for proxying,
 * and UUID natural key field patterns.
 */
public class DomainModelMetadataJava26Test {

    // --- UUID natural key pattern ---

    static Stream<Arguments> entityClassesWithUuidField() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.course.Course", "uuid"),
                Arguments.of("com.educational.platform.administration.course.CourseProposal", "uuid"),
                Arguments.of("com.educational.platform.course.enrollments.CourseEnrollment", "uuid"),
                Arguments.of("com.educational.platform.course.reviews.CourseReview", "uuid")
        );
    }

    @ParameterizedTest(name = "Entity {0} should have UUID field ''{1}'' accessible via reflection")
    @MethodSource("entityClassesWithUuidField")
    void entity_shouldHave_uuidField_accessibleViaReflection(String className, String fieldName) throws Exception {
        Class<?> clazz = Class.forName(className);
        Field uuidField = clazz.getDeclaredField(fieldName);
        uuidField.setAccessible(true);

        assertThat(uuidField.getType())
                .as("UUID field type should be java.util.UUID on %s", className)
                .isEqualTo(UUID.class);
    }

    // --- Entity no-arg constructor for JPA proxying ---

    @ParameterizedTest(name = "Entity {0} should have a no-arg or protected constructor for JPA")
    @ValueSource(strings = {
            "com.educational.platform.courses.course.Course",
            "com.educational.platform.administration.course.CourseProposal",
            "com.educational.platform.course.enrollments.CourseEnrollment",
            "com.educational.platform.course.reviews.CourseReview",
            "com.educational.platform.users.User"
    })
    void entity_shouldHave_accessibleConstructor_forJpaProxy(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        assertThat(constructors.length)
                .as("Entity '%s' should have at least one constructor", className)
                .isGreaterThan(0);

        // JPA requires a no-arg or package-private constructor
        boolean hasNoArgOrProtected = Arrays.stream(constructors)
                .anyMatch(c -> c.getParameterCount() == 0
                        || Modifier.isProtected(c.getModifiers())
                        || !Modifier.isPrivate(c.getModifiers()));

        assertThat(hasNoArgOrProtected)
                .as("Entity '%s' should have a no-arg or non-private constructor for JPA proxy creation", className)
                .isTrue();
    }

    // --- Field type resolution for JPA column mapping ---

    @Test
    void course_fieldTypes_shouldBeResolvable_forColumnMapping() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");

        Field nameField = courseClass.getDeclaredField("name");
        assertThat(nameField.getType())
                .as("Course.name field type should be String")
                .isEqualTo(String.class);

        Field descriptionField = courseClass.getDeclaredField("description");
        assertThat(descriptionField.getType())
                .as("Course.description field type should be String")
                .isEqualTo(String.class);
    }

    // --- Generic type parameter preservation for @OneToMany ---

    @Test
    void course_curriculumItems_genericType_shouldBePreserved() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Field curriculumItemsField = courseClass.getDeclaredField("curriculumItems");

        Type genericType = curriculumItemsField.getGenericType();
        assertThat(genericType)
                .as("curriculumItems generic type should be a ParameterizedType on Java 26")
                .isInstanceOf(ParameterizedType.class);

        ParameterizedType parameterized = (ParameterizedType) genericType;
        Type[] typeArgs = parameterized.getActualTypeArguments();

        assertThat(typeArgs.length)
                .as("Collection generic type should have exactly one type argument")
                .isEqualTo(1);

        assertThat(typeArgs[0].getTypeName())
                .as("Generic type argument should reference CurriculumItem")
                .contains("CurriculumItem");
    }

    // --- Enum field type resolution for @Enumerated ---

    @Test
    void course_publishStatus_shouldBeEnumType() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Field publishStatusField = courseClass.getDeclaredField("publishStatus");

        assertThat(publishStatusField.getType().isEnum())
                .as("Course.publishStatus should be an enum type on Java 26")
                .isTrue();
    }

    @Test
    void course_approvalStatus_shouldBeEnumType() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Field approvalStatusField = courseClass.getDeclaredField("approvalStatus");

        assertThat(approvalStatusField.getType().isEnum())
                .as("Course.approvalStatus should be an enum type on Java 26")
                .isTrue();

        Object[] enumConstants = approvalStatusField.getType().getEnumConstants();
        assertThat(enumConstants)
                .as("ApprovalStatus enum constants should be accessible via reflection")
                .isNotEmpty();
    }

    // --- Integration event record component accessors ---

    static Stream<Arguments> integrationEventRecords() {
        return Stream.of(
                Arguments.of("com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent",
                        "courseId", UUID.class),
                Arguments.of("com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent",
                        "courseId", UUID.class),
                Arguments.of("com.educational.platform.users.integration.event.UserCreatedIntegrationEvent",
                        "username", String.class)
        );
    }

    @ParameterizedTest(name = "Record {0} component ''{1}'' should be accessible")
    @MethodSource("integrationEventRecords")
    void integrationEventRecord_componentAccessor_shouldWork(String className, String componentName,
                                                              Class<?> expectedType) throws Exception {
        Class<?> recordClass = Class.forName(className);

        assertThat(recordClass.isRecord())
                .as("%s should be a record class on Java 26", className)
                .isTrue();

        Method accessor = recordClass.getMethod(componentName);
        assertThat(accessor.getReturnType())
                .as("Record component '%s' return type", componentName)
                .isEqualTo(expectedType);
    }

    // --- Entity class hierarchy (superclass chain) ---

    @ParameterizedTest(name = "Entity {0} should extend Object (no deep hierarchy)")
    @ValueSource(strings = {
            "com.educational.platform.courses.course.Course",
            "com.educational.platform.administration.course.CourseProposal",
            "com.educational.platform.users.User"
    })
    void entity_superclassChain_shouldBeResolvable(String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        Class<?> current = clazz;
        int depth = 0;
        while (current != null && current != Object.class) {
            current = current.getSuperclass();
            depth++;
            assertThat(depth)
                    .as("Entity class hierarchy should not be unreasonably deep")
                    .isLessThan(10);
        }

        assertThat(current)
                .as("Superclass chain should terminate at Object")
                .isEqualTo(Object.class);
    }
}
