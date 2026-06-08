package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the {@code junit-jupiter-params} dependency added in this PR
 * enables actual parameterized test execution — not just classpath presence.
 * <p>
 * {@link NewTestDependencyClasspathTest} verifies the dependency classes are
 * loadable. This test verifies the JUnit 5 engine correctly discovers and
 * executes parameterized tests using all major argument source types:
 * {@code @ValueSource}, {@code @CsvSource}, {@code @MethodSource},
 * {@code @EnumSource}, and {@code @NullAndEmptySource}.
 * <p>
 * On Java 26 with the upgraded JUnit platform, parameterized tests exercise
 * the engine's argument resolution and injection code paths, which rely on
 * reflection that could be affected by Java 26 class file changes.
 */
public class ParameterizedTestIntegrationTest {

    // --- @ValueSource ---

    @ParameterizedTest(name = "ValueSource integer: {0}")
    @ValueSource(ints = {26, 70, 44})
    void valueSource_withInts_shouldExecute(int value) {
        assertThat(value).isGreaterThan(0);
    }

    @ParameterizedTest(name = "ValueSource string: {0}")
    @ValueSource(strings = {"Java 26", "Gradle 9.5.1", "ArchUnit 1.4.2"})
    void valueSource_withStrings_shouldExecute(String value) {
        assertThat(value).isNotBlank();
    }

    // --- @CsvSource ---

    @ParameterizedTest(name = "CsvSource: Java {0} has class file major version {1}")
    @CsvSource({
            "26, 70",
            "25, 69",
            "21, 65",
            "17, 61",
            "11, 55"
    })
    void csvSource_shouldResolve_multipleColumns(int javaVersion, int majorVersion) {
        assertThat(majorVersion).isEqualTo(44 + javaVersion);
    }

    // --- @MethodSource ---

    static Stream<Arguments> versionCompatibilityMatrix() {
        return Stream.of(
                Arguments.of("Java", 26, "VERSION_26"),
                Arguments.of("Gradle", 9, "9.5.1"),
                Arguments.of("ArchUnit", 1, "1.4.2")
        );
    }

    @ParameterizedTest(name = "MethodSource: {0} major={1} version={2}")
    @MethodSource("versionCompatibilityMatrix")
    void methodSource_shouldResolve_complexArguments(String name, int major, String version) {
        assertThat(name).isNotBlank();
        assertThat(major).isPositive();
        assertThat(version).isNotBlank();
    }

    // --- @EnumSource ---

    enum UpgradedComponent {
        JAVA, GRADLE, ARCHUNIT
    }

    @ParameterizedTest(name = "EnumSource: {0}")
    @EnumSource(UpgradedComponent.class)
    void enumSource_shouldIterate_allValues(UpgradedComponent component) {
        assertThat(component).isNotNull();
    }

    @ParameterizedTest(name = "EnumSource filtered: {0}")
    @EnumSource(value = UpgradedComponent.class, names = {"JAVA", "GRADLE"})
    void enumSource_filtered_shouldOnlyInclude_specifiedValues(UpgradedComponent component) {
        assertThat(component).isIn(UpgradedComponent.JAVA, UpgradedComponent.GRADLE);
    }

    // --- @NullAndEmptySource ---

    @ParameterizedTest(name = "NullAndEmptySource: ''{0}''")
    @NullAndEmptySource
    void nullAndEmptySource_shouldProvide_nullAndEmpty(String value) {
        assertThat(value == null || value.isEmpty()).isTrue();
    }

    @ParameterizedTest(name = "NullAndEmptySource + ValueSource: ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void combinedSources_shouldProvide_allVariants(String value) {
        assertThat(value == null || value.isBlank()).isTrue();
    }

    // --- Verify total test count ---

    @Test
    void parameterizedTestAnnotation_shouldBe_retainedAtRuntime() {
        long parameterizedMethods = java.util.Arrays.stream(
                        ParameterizedTestIntegrationTest.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ParameterizedTest.class))
                .count();

        assertThat(parameterizedMethods)
                .as("@ParameterizedTest annotation should be retained at runtime on Java 26")
                .isGreaterThanOrEqualTo(7);
    }
}
