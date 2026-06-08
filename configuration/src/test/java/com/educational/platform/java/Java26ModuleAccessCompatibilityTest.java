package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that Java 26's module system and strong encapsulation do not
 * break the reflection-based patterns used by Spring and Hibernate.
 * <p>
 * Java's module system has progressively tightened access:
 * <ul>
 *   <li>Java 16: strong encapsulation by default for JDK internals</li>
 *   <li>Java 17+: --illegal-access removed entirely</li>
 *   <li>Java 26: potential further restrictions on cross-module reflection</li>
 * </ul>
 * <p>
 * The educational platform uses unnamed modules (classpath-based), which grants
 * full reflective access to other unnamed module classes. This test verifies
 * that this contract is maintained on Java 26 — if it broke, all JPA entity
 * mapping and Spring DI would fail.
 * <p>
 * {@link Java26DeepReflectionAccessTest} tests field-level access.
 * This test focuses on <em>module boundary</em> access: ensuring classes from
 * different packages can reflectively access each other as Spring/JPA requires.
 */
public class Java26ModuleAccessCompatibilityTest {

    @Test
    void applicationClasses_shouldBeIn_unnamedModule() throws Exception {
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");

        Module module = courseClass.getModule();
        assertThat(module.isNamed())
                .as("Application classes should be in the unnamed module (classpath-based)")
                .isFalse();
    }

    @Test
    void crossPackageReflection_shouldWork_betweenBoundedContexts() throws Exception {
        // Simulate Spring's cross-module DI: configuration module accessing courses module
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Class<?> userClass = Class.forName("com.educational.platform.users.User");

        // Cross-package field access (Spring/Hibernate does this constantly)
        Field courseIdField = courseClass.getDeclaredField("id");
        courseIdField.setAccessible(true);

        Field userIdField = userClass.getDeclaredField("id");
        userIdField.setAccessible(true);

        assertThat(courseIdField.getType())
                .as("Cross-package field type resolution should work")
                .isNotNull();

        assertThat(userIdField.getType())
                .as("Cross-package field type resolution should work for User")
                .isNotNull();
    }

    @Test
    void crossPackageConstructorAccess_shouldWork() throws Exception {
        // Hibernate instantiates entities from different packages
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");
        Constructor<?> constructor = courseClass.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatCode(() -> constructor.newInstance())
                .as("Cross-package constructor invocation should work on Java 26")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Class {0} should be cross-module accessible")
    @ValueSource(strings = {
            "com.educational.platform.courses.course.Course",
            "com.educational.platform.administration.course.CourseProposal",
            "com.educational.platform.course.enrollments.CourseEnrollment",
            "com.educational.platform.course.reviews.CourseReview",
            "com.educational.platform.users.User",
            "com.educational.platform.common.exception.ResourceNotFoundException"
    })
    void entityClass_shouldBeAccessible_fromTestModule(String className) throws Exception {
        Class<?> clazz = Class.forName(className);

        // Verify all reflection operations that Spring/JPA perform
        assertThatCode(() -> {
            clazz.getDeclaredFields();
            clazz.getDeclaredMethods();
            clazz.getDeclaredConstructors();
            clazz.getAnnotations();
            clazz.getInterfaces();
            clazz.getSuperclass();
            clazz.getPackageName();
        }).as("Full reflection access on %s should work from test module on Java 26", className)
                .doesNotThrowAnyException();
    }

    @Test
    void jdkModuleAccess_shouldAllow_reflectionOnStandardClasses() {
        // Verify that access to java.base classes (used by JPA mapping) still works
        assertThatCode(() -> {
            Field valueField = String.class.getDeclaredField("value");
            // Note: accessing internal JDK fields requires opens; this is expected to fail
            // But the METHOD should not throw - it should return the field
            assertThat(valueField).isNotNull();
        }).as("Accessing java.lang.String field metadata should not throw on Java 26")
                .doesNotThrowAnyException();
    }

    @Test
    void methodHandleLookup_shouldWork_forDomainClasses() throws Exception {
        // MethodHandles.Lookup is used by modern frameworks as an alternative to reflection
        Class<?> courseClass = Class.forName("com.educational.platform.courses.course.Course");

        var lookup = java.lang.invoke.MethodHandles.privateLookupIn(
                courseClass, java.lang.invoke.MethodHandles.lookup());

        assertThat(lookup)
                .as("MethodHandles.privateLookupIn should work for unnamed-module classes on Java 26")
                .isNotNull();
    }

    @Test
    void springAnnotation_shouldBeAccessible_fromApplicationModule() throws Exception {
        // Verify Spring annotations (in a different package) are accessible
        Class<?> componentAnnotation = Class.forName("org.springframework.stereotype.Component");
        Class<?> courseFactory = Class.forName("com.educational.platform.courses.course.CourseFactory");

        boolean hasComponent = courseFactory.isAnnotationPresent(
                componentAnnotation.asSubclass(java.lang.annotation.Annotation.class));

        assertThat(hasComponent)
                .as("Spring @Component should be accessible cross-module on Java 26")
                .isTrue();
    }

    @Test
    void proxyCreation_shouldWork_forDomainInterfaces() {
        // Spring creates JDK proxies for interfaces — verify this works on Java 26
        assertThatCode(() -> {
            Class<?> repoInterface = Class.forName(
                    "org.springframework.data.jpa.repository.JpaRepository");

            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    repoInterface.getClassLoader(),
                    new Class<?>[]{repoInterface},
                    (p, method, args) -> null
            );

            assertThat(proxy).isNotNull();
            assertThat(java.lang.reflect.Proxy.isProxyClass(proxy.getClass())).isTrue();
        }).as("JDK Proxy creation for repository interfaces should work on Java 26")
                .doesNotThrowAnyException();
    }
}
