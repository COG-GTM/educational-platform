package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that JDK dynamic proxies, {@link MethodHandle}, and
 * {@link VarHandle} work correctly on Java 26 compiled classes.
 * <p>
 * Spring, Hibernate, and Mockito all rely on dynamic dispatch mechanisms:
 * <ul>
 *   <li>JDK dynamic proxies — used by Spring AOP for interface-based beans</li>
 *   <li>MethodHandles — used by Hibernate 6+ and Spring for reflective access</li>
 *   <li>VarHandle — used by concurrent data structures in the JDK</li>
 * </ul>
 * Regressions in these mechanisms on Java 26 would break transaction proxies,
 * entity field access, and concurrent utilities.
 */
public class Java26ProxyAndDynamicDispatchTest {

    // --- JDK Dynamic Proxy on Java 26 interfaces ---

    interface CourseService {
        String findCourseById(UUID id);
        void enrollStudent(UUID courseId, UUID studentId);
    }

    @Test
    void jdkProxy_shouldCreateProxy_forInterfaceCompiledWithJava26() {
        UUID testId = UUID.randomUUID();

        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("findCourseById")) {
                return "Course-" + args[0];
            }
            return null;
        };

        CourseService proxy = (CourseService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{CourseService.class},
                handler
        );

        assertThat(proxy.findCourseById(testId)).isEqualTo("Course-" + testId);
        assertThat(Proxy.isProxyClass(proxy.getClass())).isTrue();
    }

    @Test
    void jdkProxy_shouldHandle_voidMethods() {
        InvocationHandler handler = (proxy, method, args) -> null;

        CourseService proxy = (CourseService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{CourseService.class},
                handler
        );

        assertThatCode(() -> proxy.enrollStudent(UUID.randomUUID(), UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    // --- Multi-interface proxy ---

    interface Identifiable {
        UUID getId();
    }

    interface Nameable {
        String getName();
    }

    @Test
    void jdkProxy_shouldSupportMultipleInterfaces_onJava26() {
        UUID id = UUID.randomUUID();
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getName" -> "Test Entity";
            default -> null;
        };

        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{Identifiable.class, Nameable.class},
                handler
        );

        assertThat(proxy).isInstanceOf(Identifiable.class);
        assertThat(proxy).isInstanceOf(Nameable.class);
        assertThat(((Identifiable) proxy).getId()).isEqualTo(id);
        assertThat(((Nameable) proxy).getName()).isEqualTo("Test Entity");
    }

    // --- MethodHandle on Java 26 ---

    @Test
    void methodHandle_shouldInvoke_staticMethod_onJava26() throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findStatic(
                UUID.class, "randomUUID", MethodType.methodType(UUID.class));

        UUID result = (UUID) mh.invoke();
        assertThat(result).isNotNull();
    }

    @Test
    void methodHandle_shouldInvoke_virtualMethod_onJava26() throws Throwable {
        MethodHandle mh = MethodHandles.lookup().findVirtual(
                String.class, "length", MethodType.methodType(int.class));

        int length = (int) mh.invoke("Java 26");
        assertThat(length).isEqualTo(7);
    }

    @Test
    void methodHandle_shouldAccessConstructor_onJava26() throws Throwable {
        MethodHandle ctorMh = MethodHandles.lookup().findConstructor(
                StringBuilder.class, MethodType.methodType(void.class, String.class));

        StringBuilder sb = (StringBuilder) ctorMh.invoke("Java 26");
        assertThat(sb.toString()).isEqualTo("Java 26");
    }

    // --- MethodHandle on production classes ---

    @ParameterizedTest(name = "MethodHandle should resolve constructor for: {0}")
    @ValueSource(strings = {
            "com.educational.platform.common.exception.ResourceNotFoundException"
    })
    void methodHandle_shouldResolveConstructor_forProductionClass(String className) {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName(className);
            var ctors = clazz.getDeclaredConstructors();
            assertThat(ctors.length).isGreaterThan(0);

            var ctor = ctors[0];
            ctor.setAccessible(true);

            MethodHandle mh = MethodHandles.lookup().unreflectConstructor(ctor);
            assertThat(mh).isNotNull();
        }).as("MethodHandle should resolve constructor for '%s' on Java 26", className)
                .doesNotThrowAnyException();
    }

    // --- VarHandle on Java 26 ---

    static class MutableEntity {
        volatile int version;
        volatile String name;
    }

    @Test
    void varHandle_shouldAccessField_onJava26CompiledClass() throws Throwable {
        VarHandle vh = MethodHandles.lookup().findVarHandle(
                MutableEntity.class, "version", int.class);

        MutableEntity entity = new MutableEntity();
        vh.set(entity, 42);

        assertThat((int) vh.get(entity)).isEqualTo(42);
    }

    @Test
    void varHandle_compareAndSet_shouldWork_onJava26() throws Throwable {
        VarHandle vh = MethodHandles.lookup().findVarHandle(
                MutableEntity.class, "version", int.class);

        MutableEntity entity = new MutableEntity();
        entity.version = 1;

        boolean swapped = vh.compareAndSet(entity, 1, 2);
        assertThat(swapped).isTrue();
        assertThat(entity.version).isEqualTo(2);

        boolean notSwapped = vh.compareAndSet(entity, 1, 3);
        assertThat(notSwapped).isFalse();
        assertThat(entity.version).isEqualTo(2);
    }

    @Test
    void varHandle_shouldAccessStringField_onJava26() throws Throwable {
        VarHandle vh = MethodHandles.lookup().findVarHandle(
                MutableEntity.class, "name", String.class);

        MutableEntity entity = new MutableEntity();
        vh.set(entity, "Java 26 Entity");

        assertThat((String) vh.get(entity)).isEqualTo("Java 26 Entity");
    }
}
