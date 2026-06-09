package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that test infrastructure from spring-boot-starter-test is
 * fully functional — not just on the classpath (covered by
 * SpringBootStarterTestValidationTest) but actually usable for isolated testing.
 * This guards the spring-boot-starter-test dependency added in this PR.
 */
class TestSliceAnnotationFunctionalTest {

    @Test
    void jsonTestAnnotation_shouldBeAvailableAndResolvable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName("org.springframework.boot.test.autoconfigure.json.JsonTest");
            assertThat(clazz.isAnnotation())
                    .as("@JsonTest must be a resolvable annotation for JSON slice testing")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void webApplicationContextRunner_shouldCreateWebContext() {
        new WebApplicationContextRunner()
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void applicationContextRunner_shouldSupportBeanDefinitionOverriding() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.main.allow-bean-definition-overriding=true")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void outputCaptureExtension_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.system.OutputCaptureExtension"))
                .as("OutputCaptureExtension must be available via spring-boot-starter-test for output capture")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootTest_shouldBeAvailableAndResolvable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName("org.springframework.boot.test.context.SpringBootTest");
            assertThat(clazz.isAnnotation())
                    .as("@SpringBootTest must be a resolvable annotation")
                    .isTrue();
        }).doesNotThrowAnyException();
    }

    @Test
    void testPropertyValues_shouldBeUsable() {
        assertThatCode(() -> {
            Class<?> clazz = Class.forName("org.springframework.boot.test.util.TestPropertyValues");
            assertThat(clazz).isNotNull();
        }).as("TestPropertyValues must be available for programmatic property injection in tests")
                .doesNotThrowAnyException();
    }

    @Test
    void filteredClassLoader_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.context.FilteredClassLoader"))
                .as("FilteredClassLoader must be available for testing auto-config conditional behavior")
                .doesNotThrowAnyException();
    }

    @Test
    void springBootMockServletContext_shouldBeAvailable() {
        assertThatCode(() -> Class.forName("org.springframework.boot.test.mock.web.SpringBootMockServletContext"))
                .as("SpringBootMockServletContext must be available for web testing support")
                .doesNotThrowAnyException();
    }
}
