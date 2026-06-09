package com.educational.platform.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.Extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that spring-boot-starter-test brings the JUnit 5 extension model
 * infrastructure needed for Spring Boot test integration. The extension model
 * is the mechanism by which {@code @SpringBootTest} bootstraps the application
 * context: {@code SpringExtension} implements multiple JUnit 5 extension points
 * (BeforeAllCallback, ParameterResolver, etc.). If the extension model classes
 * are missing or incompatible, {@code @SpringBootTest} fails silently or with
 * cryptic errors.
 * <p>
 * Complements {@link SpringBootTestJUnitPlatformCompatibilityTest} which validates
 * that SpringExtension and MockitoExtension implement the Extension interface.
 * This test validates the extension point contracts themselves.
 */
class StarterTestJUnit5ExtensionModelTest {

    @Test
    void parameterResolver_shouldBeAvailableOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.junit.jupiter.api.extension.ParameterResolver"))
                .as("ParameterResolver must be on the test classpath — "
                        + "SpringExtension uses it to inject ApplicationContext, "
                        + "MockitoBean, and other test dependencies into test methods")
                .doesNotThrowAnyException();
    }

    @Test
    void beforeAllCallback_shouldBeAvailableOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.junit.jupiter.api.extension.BeforeAllCallback"))
                .as("BeforeAllCallback must be on the test classpath — "
                        + "SpringExtension uses it to initialize the application context "
                        + "before any @SpringBootTest methods execute")
                .doesNotThrowAnyException();
    }

    @Test
    void afterAllCallback_shouldBeAvailableOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.junit.jupiter.api.extension.AfterAllCallback"))
                .as("AfterAllCallback must be on the test classpath — "
                        + "SpringExtension uses it to close the application context "
                        + "after all tests in a class have run")
                .doesNotThrowAnyException();
    }

    @Test
    void testInstancePostProcessor_shouldBeAvailableOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.junit.jupiter.api.extension.TestInstancePostProcessor"))
                .as("TestInstancePostProcessor must be on the test classpath — "
                        + "SpringExtension uses it to inject Spring beans into test instances")
                .doesNotThrowAnyException();
    }

    @Test
    void springExtension_shouldImplementParameterResolver() throws ClassNotFoundException {
        Class<?> springExtension = Class.forName(
                "org.springframework.test.context.junit.jupiter.SpringExtension");
        Class<?> parameterResolver = Class.forName(
                "org.junit.jupiter.api.extension.ParameterResolver");
        assertThat(parameterResolver.isAssignableFrom(springExtension))
                .as("SpringExtension must implement ParameterResolver — "
                        + "this is how Spring injects ApplicationContext, Environment, "
                        + "and other beans into @SpringBootTest method parameters")
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
                        + "without this, the application context would not be initialized "
                        + "before test class execution")
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
                        + "without this, the application context would not be cleaned up "
                        + "after test class execution")
                .isTrue();
    }

    @Test
    void extensionContext_shouldBeAvailableOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.junit.jupiter.api.extension.ExtensionContext"))
                .as("ExtensionContext must be on the test classpath — "
                        + "it is the core API through which extensions interact with "
                        + "the JUnit 5 runtime")
                .doesNotThrowAnyException();
    }

    @Test
    void conditionEvaluationResult_shouldBeAvailableOnTestClasspath() {
        assertThatCode(() -> Class.forName(
                "org.junit.jupiter.api.extension.ConditionEvaluationResult"))
                .as("ConditionEvaluationResult must be on the test classpath — "
                        + "it is used by @DisabledIf, @EnabledIf and other conditional "
                        + "test execution annotations")
                .doesNotThrowAnyException();
    }
}
