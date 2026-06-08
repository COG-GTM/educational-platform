package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies that Spring Boot 4.0.1 and its transitive dependencies are
 * present on the classpath and compatible with Java 26.
 * <p>
 * Spring Boot 4.0.1 works with Java 26 out of the box. This test ensures
 * no classpath issues arise from the Java 26 + Gradle 9.5.1 upgrade and
 * that key Spring framework classes are resolvable at runtime.
 */
public class SpringBootDependencyCompatibilityTest {

    @ParameterizedTest(name = "Spring class should be loadable: {0}")
    @ValueSource(strings = {
            "org.springframework.boot.autoconfigure.SpringBootApplication",
            "org.springframework.boot.SpringApplication",
            "org.springframework.context.annotation.Configuration",
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository",
            "org.springframework.beans.factory.annotation.Autowired",
            "org.springframework.transaction.annotation.Transactional"
    })
    void springClass_shouldBeLoadable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("Spring class '%s' should be loadable on Java 26", className)
                .doesNotThrowAnyException();
    }

    @Test
    void springBoot_applicationClass_shouldBeInstantiable() {
        assertThatCode(() -> {
            Class<?> springAppClass = Class.forName("org.springframework.boot.SpringApplication");
            assertThat(springAppClass.getDeclaredConstructors())
                    .as("SpringApplication should have constructors")
                    .isNotEmpty();
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Jakarta API class should be loadable: {0}")
    @ValueSource(strings = {
            "jakarta.persistence.Entity",
            "jakarta.persistence.Id",
            "jakarta.persistence.GeneratedValue",
            "jakarta.annotation.Nonnull",
            "jakarta.annotation.Nullable"
    })
    void jakartaClass_shouldBeLoadable(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("Jakarta API class '%s' should be present (Spring Boot 4.x uses Jakarta namespace)", className)
                .doesNotThrowAnyException();
    }

    @Test
    void springDataJpa_shouldBeOnClasspath() {
        assertThatCode(() ->
                Class.forName("org.springframework.data.jpa.repository.JpaRepository")
        ).as("Spring Data JPA should be on classpath for Java 26 runtime")
                .doesNotThrowAnyException();
    }

    @Test
    void liquibase_shouldBeOnClasspath() {
        assertThatCode(() ->
                Class.forName("liquibase.Liquibase")
        ).as("Liquibase should be on classpath (used for DB migrations)")
                .doesNotThrowAnyException();
    }

    @Test
    void springSecurity_shouldBeOnClasspath() {
        assertThatCode(() ->
                Class.forName("org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")
        ).as("Spring Security should be on classpath for Java 26 runtime")
                .doesNotThrowAnyException();
    }

    @Test
    void springWeb_shouldBeOnClasspath() {
        assertThatCode(() ->
                Class.forName("org.springframework.web.servlet.DispatcherServlet")
        ).as("Spring Web MVC DispatcherServlet should be on classpath for Java 26 runtime")
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Project bounded context class should be loadable: {0}")
    @ValueSource(strings = {
            "com.educational.platform.courses.course.Course",
            "com.educational.platform.administration.course.CourseProposal",
            "com.educational.platform.course.enrollments.CourseEnrollment",
            "com.educational.platform.course.reviews.CourseReview",
            "com.educational.platform.users.User"
    })
    void projectDomainClass_shouldBeLoadable_withJava26Runtime(String className) {
        assertThatCode(() -> Class.forName(className))
                .as("Domain class '%s' should be loadable under Java 26", className)
                .doesNotThrowAnyException();
    }
}
