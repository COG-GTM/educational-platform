package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that {@code SpringExtension} — the JUnit Jupiter extension that
 * powers {@code @SpringBootTest} — is available and properly structured on
 * the test classpath. {@code SpringExtension} implements multiple JUnit 5
 * extension points (ParameterResolver, BeforeAllCallback, AfterAllCallback,
 * etc.) to integrate the Spring TestContext Framework with JUnit Jupiter.
 * <p>
 * {@link StarterTestJUnit5ExtensionModelTest} validates that JUnit 5
 * extension interfaces are loadable. {@link SpringBootTestJUnitPlatformCompatibilityTest}
 * validates that {@code SpringExtension} is on the classpath. This test
 * goes deeper: it validates that {@code SpringExtension} actually
 * implements the expected JUnit 5 extension interfaces, proving the
 * integration contract between Spring's test infrastructure and JUnit 5.
 * <p>
 * Without these extension implementations, {@code @SpringBootTest} would
 * not be able to inject beans via {@code @Autowired}, manage context
 * lifecycle, or resolve test method parameters.
 */
class SpringBootTestSpringExtensionAvailabilityTest {

    @Test
    void springExtension_shouldImplementJupiterExtension() throws ClassNotFoundException {
        Class<?> springExtension = Class.forName(
                "org.springframework.test.context.junit.jupiter.SpringExtension");
        assertThat(Extension.class.isAssignableFrom(springExtension))
                .as("SpringExtension must implement JUnit Jupiter Extension interface — "
                        + "this is the base contract for JUnit 5 integration")
                .isTrue();
    }

    @Test
    void springExtension_shouldImplementParameterResolver() throws ClassNotFoundException {
        Class<?> springExtension = Class.forName(
                "org.springframework.test.context.junit.jupiter.SpringExtension");
        Class<?> parameterResolver = Class.forName(
                "org.junit.jupiter.api.extension.ParameterResolver");
        assertThat(parameterResolver.isAssignableFrom(springExtension))
                .as("SpringExtension must implement ParameterResolver — "
                        + "this enables @Autowired injection into test method parameters "
                        + "and constructor injection for test classes")
                .isTrue();
    }

    @Test
    void springExtension_shouldImplementBeforeAllCallback() throws ClassNotFoundException {
        Class<?> springExtension = Class.forName(
                "org.springframework.test.context.junit.jupiter.SpringExtension");
        Class<?> beforeAllCallback = Class.forName(
                "org.junit.jupiter.api.extension.BeforeAllCallback");
        assertThat(beforeAllCallback.isAssignableFrom(springExtension))
                .as("SpringExtension must implement BeforeAllCallback — "
                        + "this initializes the Spring ApplicationContext before test class execution")
                .isTrue();
    }

    @Test
    void springExtension_shouldImplementAfterAllCallback() throws ClassNotFoundException {
        Class<?> springExtension = Class.forName(
                "org.springframework.test.context.junit.jupiter.SpringExtension");
        Class<?> afterAllCallback = Class.forName(
                "org.junit.jupiter.api.extension.AfterAllCallback");
        assertThat(afterAllCallback.isAssignableFrom(springExtension))
                .as("SpringExtension must implement AfterAllCallback — "
                        + "this handles context cleanup after test class execution")
                .isTrue();
    }

    @Test
    void springExtension_shouldImplementTestInstancePostProcessor() throws ClassNotFoundException {
        Class<?> springExtension = Class.forName(
                "org.springframework.test.context.junit.jupiter.SpringExtension");
        Class<?> testInstancePostProcessor = Class.forName(
                "org.junit.jupiter.api.extension.TestInstancePostProcessor");
        assertThat(testInstancePostProcessor.isAssignableFrom(springExtension))
                .as("SpringExtension must implement TestInstancePostProcessor — "
                        + "this enables @Autowired field injection on test instances")
                .isTrue();
    }

    @Test
    void springExtensionAnnotation_shouldBeAvailable() {
        assertThatCode(() -> {
            Class<?> annotation = Class.forName(
                    "org.springframework.test.context.junit.jupiter.SpringJUnitConfig");
            assertThat(annotation.isAnnotation())
                    .as("@SpringJUnitConfig (composed annotation for @ExtendWith(SpringExtension.class) + "
                            + "@ContextConfiguration) must be available as a testing convenience")
                    .isTrue();
        }).doesNotThrowAnyException();
    }
}
