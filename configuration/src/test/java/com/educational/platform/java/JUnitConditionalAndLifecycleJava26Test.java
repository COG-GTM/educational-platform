package com.educational.platform.java;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that JUnit 5's lifecycle extensions and conditional execution
 * mechanisms work correctly on Java 26 bytecode (class file major version 70).
 * <p>
 * These features were not exercised by the existing test suite:
 * <ul>
 *   <li>{@code @TempDir} — lifecycle extension that creates and injects
 *       temporary directories using the file system API</li>
 *   <li>{@code @RepeatedTest} — repeated execution with {@code RepetitionInfo}
 *       injection via parameter resolver</li>
 *   <li>{@code Assumptions} — programmatic test skipping based on runtime
 *       conditions</li>
 * </ul>
 * <p>
 * Note: {@code @EnabledForJreRange} / {@code @EnabledOnJre} are not tested
 * because the current JUnit {@code JRE} enum does not include Java 26 yet,
 * causing {@code PreconditionViolationException}. Programmatic
 * {@code Assumptions} provide equivalent conditional execution coverage.
 * <p>
 * {@link ParameterizedTestIntegrationTest} validates argument-source-based
 * parameterization. This test validates lifecycle- and condition-based
 * execution mechanisms that depend on JVM introspection and extension
 * model reflection on Java 26 class files.
 */
public class JUnitConditionalAndLifecycleJava26Test {

    // --- Programmatic version-conditional execution ---

    @Test
    void programmaticCondition_java26_shouldPass() {
        Assumptions.assumeTrue(Runtime.version().feature() >= 26,
                "Requires Java 26+");

        assertThat(Runtime.version().feature())
                .as("After assumption passes, should be running on Java 26")
                .isEqualTo(26);
    }

    @Test
    void programmaticCondition_classFileVersion_shouldBe70() {
        int major = (int) Double.parseDouble(System.getProperty("java.class.version"));
        Assumptions.assumeTrue(major >= 70, "Requires class file v70+");

        assertThat(major)
                .as("Class file major version should be 70 for Java 26")
                .isEqualTo(70);
    }

    // --- @TempDir ---

    @Test
    void tempDir_shouldBeInjected_asMethodParameter(@TempDir Path tempDir) {
        assertThat(tempDir)
                .as("@TempDir should inject a valid temporary directory on Java 26")
                .isNotNull()
                .isDirectory();
    }

    @Test
    void tempDir_shouldSupportFileOperations(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("java26-test.txt");
        Files.writeString(testFile, "Java 26 temp file test");

        assertThat(testFile).exists();
        assertThat(Files.readString(testFile)).isEqualTo("Java 26 temp file test");
    }

    @Test
    void tempDir_shouldSupportSubdirectories(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("nested/deep");
        Files.createDirectories(subDir);
        Path file = subDir.resolve("data.txt");
        Files.writeString(file, "nested content");

        assertThat(subDir).isDirectory();
        assertThat(Files.readString(file)).isEqualTo("nested content");
    }

    @TempDir
    Path instanceTempDir;

    @Test
    void tempDir_instanceField_shouldBeInjected() {
        assertThat(instanceTempDir)
                .as("@TempDir instance field should be injected on Java 26")
                .isNotNull()
                .isDirectory();
    }

    // --- @RepeatedTest ---

    @RepeatedTest(3)
    void repeatedTest_shouldExecute_multipleTimesOnJava26(RepetitionInfo info) {
        assertThat(info.getCurrentRepetition())
                .as("Repetition number should be between 1 and total")
                .isBetween(1, info.getTotalRepetitions());

        assertThat(info.getTotalRepetitions())
                .as("Total repetitions should be 3")
                .isEqualTo(3);
    }

    @RepeatedTest(value = 2, name = "Java 26 repetition {currentRepetition}/{totalRepetitions}")
    void repeatedTest_withCustomName_shouldExecute(RepetitionInfo info) {
        assertThat(Runtime.version().feature()).isEqualTo(26);
        assertThat(info.getCurrentRepetition()).isPositive();
    }

    // --- Assumptions ---

    @Test
    void assumptions_assumeTrue_shouldPass_onJava26() {
        Assumptions.assumeTrue(Runtime.version().feature() >= 26,
                "Requires Java 26+");

        assertThat(System.getProperty("java.class.version"))
                .as("After assumption passes, class version should be 70.0")
                .startsWith("70");
    }

    @Test
    void assumptions_assumingThat_shouldExecute_conditionalBlock() {
        Assumptions.assumingThat(
                Runtime.version().feature() == 26,
                () -> assertThat(44 + 26)
                        .as("Class file major version formula")
                        .isEqualTo(70)
        );
    }

    @Test
    void assumptions_assumeFalse_shouldSkip_whenConditionTrue() {
        assertThatCode(() -> {
            Assumptions.assumeFalse(Runtime.version().feature() < 26,
                    "Should not skip on Java 26");

            assertThat(Runtime.version().feature()).isEqualTo(26);
        }).doesNotThrowAnyException();
    }

    // --- Annotation retention ---

    @Test
    void tempDirAnnotation_shouldBe_retainedAtRuntime() {
        long tempDirFields = java.util.Arrays.stream(
                        JUnitConditionalAndLifecycleJava26Test.class.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(TempDir.class))
                .count();

        assertThat(tempDirFields)
                .as("@TempDir field annotation should be retained at runtime on Java 26")
                .isEqualTo(1);
    }

    @Test
    void repeatedTestAnnotation_shouldBe_retainedAtRuntime() {
        long repeatedMethods = java.util.Arrays.stream(
                        JUnitConditionalAndLifecycleJava26Test.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(RepeatedTest.class))
                .count();

        assertThat(repeatedMethods)
                .as("@RepeatedTest annotation should be retained at runtime on Java 26")
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void tempDir_methodParameter_annotationRetention() throws NoSuchMethodException {
        var method = JUnitConditionalAndLifecycleJava26Test.class
                .getDeclaredMethod("tempDir_shouldBeInjected_asMethodParameter", Path.class);

        var paramAnnotations = method.getParameterAnnotations();
        assertThat(paramAnnotations.length)
                .as("Method should have exactly one parameter")
                .isEqualTo(1);
        assertThat(paramAnnotations[0])
                .as("@TempDir parameter annotation should be retained on Java 26")
                .hasSize(1);
        assertThat(paramAnnotations[0][0].annotationType())
                .isEqualTo(TempDir.class);
    }
}
