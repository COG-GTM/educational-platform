package com.educational.platform.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that the spring-boot-starter-test dependency added in
 * configuration/build.gradle.kts correctly makes test infrastructure
 * classes available on the test classpath. Without the dependency,
 * these classes would not resolve.
 */
class SpringBootStarterTestValidationTest {

    @Test
    void springBootTestAnnotation_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.context.SpringBootTest"))
                .as("@SpringBootTest must be available via spring-boot-starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void mockBeanAnnotation_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.context.bean.override.mockito.MockitoBean"))
                .as("@MockitoBean must be available via spring-boot-starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void springTestContextFramework_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.test.context.TestContext"))
                .as("Spring TestContext framework must be available via spring-boot-starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void applicationContextRunner_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.context.runner.ApplicationContextRunner"))
                .as("ApplicationContextRunner must be available via spring-boot-starter-test")
                .doesNotThrowAnyException();
    }

    @Test
    void assertJ_shouldBeOnClasspath() {
        assertThat(org.assertj.core.api.Assertions.class)
                .as("AssertJ must be available via spring-boot-starter-test transitive dependency")
                .isNotNull();
    }

    @Test
    void mockitoCore_shouldBeOnClasspath() {
        assertThatCode(() -> Class.forName("org.mockito.Mockito"))
                .as("Mockito must be available via spring-boot-starter-test transitive dependency")
                .doesNotThrowAnyException();
    }
}
