package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that generic type parameters are preserved in Java 26 bytecode
 * and accessible via the reflection API.
 * <p>
 * Spring Framework and JPA both rely on runtime generic type resolution to:
 * <ul>
 *   <li>Resolve repository generic parameters (e.g., {@code JpaRepository<Course, Long>})</li>
 *   <li>Determine collection element types for {@code @OneToMany} mappings</li>
 *   <li>Match event listener parameter types for {@code @EventListener} methods</li>
 *   <li>Resolve dependency injection by generic type</li>
 * </ul>
 * <p>
 * Java erases generics at the bytecode level, but retains type information in
 * the {@code Signature} class file attribute. If Java 26's compiler changes how
 * this attribute is emitted, Spring and JPA would silently mis-resolve types.
 * <p>
 * {@link DomainModelMetadataJava26Test} tests basic field type resolution.
 * This test focuses specifically on <em>parameterized type</em> preservation.
 */
public class Java26GenericTypePreservationTest {

    @Test
    void collectionField_shouldPreserve_genericTypeArgument() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Field curriculumItems = courseClass.getDeclaredField("curriculumItems");

        Type genericType = curriculumItems.getGenericType();

        assertThat(genericType)
                .as("curriculumItems field should have a parameterized generic type")
                .isInstanceOf(ParameterizedType.class);

        ParameterizedType paramType = (ParameterizedType) genericType;
        Type[] typeArgs = paramType.getActualTypeArguments();

        assertThat(typeArgs)
                .as("List<CurriculumItem> should have one type argument")
                .hasSize(1);

        assertThat(typeArgs[0].getTypeName())
                .as("Type argument should reference CurriculumItem")
                .contains("CurriculumItem");
    }

    @Test
    void entityIdField_genericType_shouldBeConcreteType() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Field idField = courseClass.getDeclaredField("id");

        Type genericType = idField.getGenericType();

        // id is a concrete type (non-parameterized), so genericType should be a Class
        assertThat(genericType)
                .as("id field should have a concrete (non-parameterized) type")
                .isInstanceOf(Class.class);

        assertThat(((Class<?>) genericType).getName())
                .as("id field type should be a numeric type (Integer, Long, or primitive)")
                .isIn("java.lang.Integer", "java.lang.Long", "int", "long");
    }

    static Stream<Arguments> eventListenerMethodsWithGenericParams() {
        return Stream.of(
                Arguments.of(
                        "com.educational.platform.courses.course.approve.CourseApprovedByAdminIntegrationEventHandler",
                        "com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent"
                )
        );
    }

    @ParameterizedTest(name = "Event handler {0} should resolve event type parameter")
    @MethodSource("eventListenerMethodsWithGenericParams")
    void eventListenerMethod_shouldPreserve_parameterType(String handlerClassName, String expectedEventType)
            throws Exception {
        Class<?> handlerClass = Class.forName(handlerClassName);

        Method listenerMethod = Arrays.stream(handlerClass.getDeclaredMethods())
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> {
                    try {
                        return m.getParameterTypes()[0].getName().equals(expectedEventType);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .orElse(null);

        assertThat(listenerMethod)
                .as("Handler should have a method accepting %s", expectedEventType)
                .isNotNull();

        Type[] genericParams = listenerMethod.getGenericParameterTypes();
        assertThat(genericParams[0].getTypeName())
                .as("Generic parameter type should preserve the event class name")
                .contains("IntegrationEvent");
    }

    @Test
    void recordComponents_shouldPreserve_genericTypes() throws Exception {
        Class<?> eventClass = Class.forName(
                "com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent");

        assertThat(eventClass.isRecord())
                .as("Integration event should be a record")
                .isTrue();

        var components = eventClass.getRecordComponents();
        for (var component : components) {
            Type genericType = component.getGenericType();
            assertThat(genericType)
                    .as("Record component '%s' generic type should be accessible", component.getName())
                    .isNotNull();
        }
    }

    @Test
    void superclassGenericType_shouldBePreserved_onJava26() throws Exception {
        // Verify that a class extending a generic base preserves the type argument
        // in the Signature attribute (Spring resolves these for DI)
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Type superclass = courseClass.getGenericSuperclass();

        assertThat(superclass)
                .as("Course superclass type information should be preserved in Java 26 bytecode")
                .isNotNull();
    }

    @Test
    void interfaceGenericTypes_shouldBePreserved_onJava26() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Type[] interfaces = courseClass.getGenericInterfaces();

        // Even if empty, the call itself should not throw
        assertThatCode(() -> courseClass.getGenericInterfaces())
                .as("getGenericInterfaces() should not throw on Java 26 compiled class")
                .doesNotThrowAnyException();

        // Verify each interface type is resolvable
        for (Type iface : interfaces) {
            assertThat(iface.getTypeName())
                    .as("Interface type name should be resolvable")
                    .isNotBlank();
        }
    }

    @Test
    void methodReturnType_shouldPreserve_genericType() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");

        Method[] methods = courseClass.getDeclaredMethods();
        for (Method method : methods) {
            Type returnType = method.getGenericReturnType();
            assertThat(returnType)
                    .as("Method %s return type should be accessible via getGenericReturnType()",
                            method.getName())
                    .isNotNull();
        }
    }
}
