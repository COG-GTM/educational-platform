package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validates that the test framework stack added by the Java 26 upgrade
 * (JUnit Jupiter Params, JUnit Jupiter Engine, AssertJ Core) functions
 * correctly in combination on Java 26 bytecode.
 * <p>
 * The PR added three new test dependencies to configuration/build.gradle.kts:
 * <ul>
 *   <li>junit-jupiter-params — enables {@code @ParameterizedTest} with various sources</li>
 *   <li>junit-jupiter-engine — JUnit 5 test engine for discovery and execution</li>
 *   <li>assertj-core — fluent assertion library used by all new upgrade tests</li>
 * </ul>
 * <p>
 * Individual classpath checks exist in {@link DependencyVersionTest} and
 * {@link DependencyResolutionVerificationTest}. This test goes further by
 * exercising the frameworks' runtime behavior on Java 26 compiled classes,
 * including reflection-heavy paths (MethodSource, Mockito proxy generation).
 */
public class TestFrameworkStackJava26Test {

    // --- JUnit Jupiter Params: @MethodSource reflection on Java 26 ---

    static Stream<Arguments> versionArguments() {
        return Stream.of(
                Arguments.of(26, 70, "Java 26 maps to class file major version 70"),
                Arguments.of(25, 69, "Java 25 maps to class file major version 69"),
                Arguments.of(21, 65, "Java 21 maps to class file major version 65")
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("versionArguments")
    void methodSource_shouldResolve_staticMethodViaReflection(int javaVersion, int expectedMajor, String description) {
        assertThat(44 + javaVersion)
                .as(description)
                .isEqualTo(expectedMajor);
    }

    @ParameterizedTest(name = "CsvSource row: javaVersion={0}, gradleMinMinor={1}")
    @CsvSource({
            "26, 4",
            "25, 2",
            "24, 0"
    })
    void csvSource_shouldParse_andInject_arguments(int javaVersion, int gradleMinMinor) {
        assertThat(javaVersion).isGreaterThanOrEqualTo(24);
        assertThat(gradleMinMinor).isGreaterThanOrEqualTo(0);
    }

    // --- Mockito proxy generation on Java 26 bytecode ---

    interface VersionChecker {
        boolean isCompatible(int javaVersion);
        String requiredGradleVersion(int javaVersion);
    }

    @Test
    void mockito_shouldCreateProxy_forInterfaceCompiledWithJava26() {
        VersionChecker checker = mock(VersionChecker.class);
        when(checker.isCompatible(26)).thenReturn(true);
        when(checker.requiredGradleVersion(26)).thenReturn("9.4.0");

        assertThat(checker.isCompatible(26)).isTrue();
        assertThat(checker.requiredGradleVersion(26)).isEqualTo("9.4.0");
        Mockito.verify(checker).isCompatible(26);
    }

    @Test
    void mockito_shouldCreateProxy_forAbstractClassCompiledWithJava26() {
        abstract class AbstractHandler {
            abstract void handle(String command);
            String name() { return "handler"; }
        }

        AbstractHandler handler = mock(AbstractHandler.class);
        when(handler.name()).thenReturn("mock-handler");

        assertThat(handler.name()).isEqualTo("mock-handler");
    }

    @Test
    void mockito_shouldSpy_onConcreteClassCompiledWithJava26() {
        var original = new java.util.ArrayList<String>();
        var spy = Mockito.spy(original);

        spy.add("Java 26");
        Mockito.verify(spy).add("Java 26");
        assertThat(spy).containsExactly("Java 26");
    }

    // --- AssertJ fluent assertions on Java 26 ---

    @Test
    void assertJ_softAssertions_shouldWork_onJava26() {
        org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();

        softly.assertThat(Runtime.version().feature()).isEqualTo(26);
        softly.assertThat(System.getProperty("java.class.version")).startsWith("70");
        softly.assertThat("VERSION_26").contains("26");

        softly.assertAll();
    }

    @Test
    void assertJ_exceptionAssertions_shouldWork_onJava26() {
        assertThatCode(() -> {
            throw new IllegalArgumentException("Java 25 is no longer supported");
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Java 25");
    }

    @Test
    void assertJ_collectionAssertions_shouldWork_withJava26Features() {
        var versions = java.util.List.of(
                new Java26LanguageFeaturesTest.VersionInfo(9, 5, 1),
                new Java26LanguageFeaturesTest.VersionInfo(9, 4, 0),
                new Java26LanguageFeaturesTest.VersionInfo(9, 2, 1)
        );

        assertThat(versions)
                .hasSize(3)
                .extracting(Java26LanguageFeaturesTest.VersionInfo::formatted)
                .containsExactly("9.5.1", "9.4.0", "9.2.1");
    }

    // --- JUnit Jupiter Engine discovery ---

    @Test
    void junitEngine_shouldDiscover_testAnnotation_viaReflection() throws Exception {
        Method[] methods = TestFrameworkStackJava26Test.class.getDeclaredMethods();

        long testMethodCount = java.util.Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(Test.class)
                        || m.isAnnotationPresent(ParameterizedTest.class))
                .count();

        assertThat(testMethodCount)
                .as("JUnit should discover test methods via reflection on Java 26 compiled class")
                .isGreaterThanOrEqualTo(8);
    }

    @Test
    void junitEngine_shouldResolve_parameterizedTestAnnotation_attributes() throws Exception {
        Method method = TestFrameworkStackJava26Test.class.getDeclaredMethod(
                "methodSource_shouldResolve_staticMethodViaReflection", int.class, int.class, String.class);

        ParameterizedTest annotation = method.getAnnotation(ParameterizedTest.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("{2}");

        MethodSource methodSource = method.getAnnotation(MethodSource.class);
        assertThat(methodSource).isNotNull();
        assertThat(methodSource.value()).containsExactly("versionArguments");
    }
}
