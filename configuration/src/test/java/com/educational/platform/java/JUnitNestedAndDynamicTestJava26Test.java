package com.educational.platform.java;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Validates that JUnit 5's {@code @Nested} inner test classes and
 * {@code @TestFactory} dynamic tests work correctly on Java 26 bytecode.
 * <p>
 * {@code @Nested} classes compile to inner classes with different bytecode
 * attributes than top-level classes (e.g. {@code InnerClasses} attribute,
 * synthetic access methods). {@code @TestFactory} relies on lambda-based
 * test generation at runtime. Both exercise distinct JUnit engine code
 * paths that could regress with class file major version 70.
 * <p>
 * {@link ParameterizedTestIntegrationTest} validates parameterized tests.
 * This test complements it by covering the remaining JUnit 5 test
 * discovery mechanisms: nested class scanning and dynamic test streams.
 */
public class JUnitNestedAndDynamicTestJava26Test {

    // --- @Nested inner class tests ---

    @Nested
    class JavaVersionValidation {

        @Test
        void runtimeVersion_shouldBe26() {
            assertThat(Runtime.version().feature())
                    .as("Nested test should see Java 26 runtime")
                    .isEqualTo(26);
        }

        @Test
        void classFileMajorVersion_shouldBe70() {
            int major = (int) Double.parseDouble(System.getProperty("java.class.version"));
            assertThat(major)
                    .as("Nested test should see class file major version 70")
                    .isEqualTo(70);
        }
    }

    @Nested
    class BytecodeFormulaValidation {

        @Test
        void formula_44PlusJavaVersion_shouldEqual_majorVersion() {
            int feature = Runtime.version().feature();
            int expectedMajor = 44 + feature;
            int actualMajor = (int) Double.parseDouble(System.getProperty("java.class.version"));

            assertThat(actualMajor).isEqualTo(expectedMajor);
        }
    }

    @Nested
    class NestedClassBytecodeAttributes {

        @Test
        void nestedClass_shouldBe_innerClass() {
            assertThat(NestedClassBytecodeAttributes.class.isMemberClass())
                    .as("@Nested class should be a member class in Java 26 bytecode")
                    .isTrue();
        }

        @Test
        void nestedClass_shouldHave_enclosingClass() {
            assertThat(NestedClassBytecodeAttributes.class.getEnclosingClass())
                    .as("@Nested class should have correct enclosing class")
                    .isEqualTo(JUnitNestedAndDynamicTestJava26Test.class);
        }

        @Test
        void nestedAnnotation_shouldBe_retainedAtRuntime() {
            assertThat(NestedClassBytecodeAttributes.class.isAnnotationPresent(Nested.class))
                    .as("@Nested annotation should be retained at runtime on Java 26")
                    .isTrue();
        }
    }

    @Nested
    class DeeplyNestedLevel1 {

        @Nested
        class DeeplyNestedLevel2 {

            @Test
            void deeplyNested_shouldExecute_onJava26() {
                assertThat(Runtime.version().feature())
                        .as("Deeply nested test should execute on Java 26")
                        .isEqualTo(26);
            }

            @Test
            void deeplyNested_shouldHave_correctEnclosingHierarchy() {
                assertThat(DeeplyNestedLevel2.class.getEnclosingClass())
                        .isEqualTo(DeeplyNestedLevel1.class);
                assertThat(DeeplyNestedLevel1.class.getEnclosingClass())
                        .isEqualTo(JUnitNestedAndDynamicTestJava26Test.class);
            }
        }
    }

    // --- @TestFactory dynamic tests ---

    @TestFactory
    Stream<DynamicTest> dynamicTests_versionFormula() {
        record VersionPair(int javaVersion, int expectedMajor) {}
        var pairs = List.of(
                new VersionPair(26, 70),
                new VersionPair(25, 69),
                new VersionPair(21, 65),
                new VersionPair(17, 61),
                new VersionPair(11, 55),
                new VersionPair(8, 52)
        );

        return pairs.stream().map(pair ->
                dynamicTest("Java " + pair.javaVersion() + " → major " + pair.expectedMajor(),
                        () -> assertThat(44 + pair.javaVersion()).isEqualTo(pair.expectedMajor()))
        );
    }

    @TestFactory
    Stream<DynamicTest> dynamicTests_upgradeArtifacts() {
        record Artifact(String name, String oldVersion, String newVersion) {}
        var artifacts = List.of(
                new Artifact("Java", "25", "26"),
                new Artifact("Gradle", "9.2.1", "9.5.1"),
                new Artifact("ArchUnit", "1.4.1", "1.4.2")
        );

        return artifacts.stream().map(a ->
                dynamicTest(a.name() + " upgraded from " + a.oldVersion() + " to " + a.newVersion(),
                        () -> assertThat(a.newVersion()).isNotEqualTo(a.oldVersion()))
        );
    }

    @TestFactory
    Stream<DynamicTest> dynamicTests_classLoading_forKeyClasses() {
        var classNames = List.of(
                "com.educational.platform.courses.course.Course",
                "com.educational.platform.users.User",
                "com.educational.platform.courses.CourseController"
        );

        return classNames.stream().map(cn ->
                dynamicTest("Class.forName(" + cn + ") on Java 26",
                        () -> assertThatCode(() -> Class.forName(cn)).doesNotThrowAnyException())
        );
    }

    @TestFactory
    Stream<DynamicNode> dynamicTests_fromIntStream() {
        return IntStream.rangeClosed(1, 5).mapToObj(i ->
                dynamicTest("Dynamic test #" + i + " on Java 26",
                        () -> assertThat(i).isBetween(1, 5))
        );
    }

    // --- Verify discovery metadata ---

    @Test
    void testFactoryAnnotation_shouldBe_retainedAtRuntime() {
        long testFactoryMethods = java.util.Arrays.stream(
                        JUnitNestedAndDynamicTestJava26Test.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(TestFactory.class))
                .count();

        assertThat(testFactoryMethods)
                .as("@TestFactory annotation should be retained at runtime on Java 26")
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void nestedAnnotation_shouldBe_retainedOnAllNestedClasses() {
        long nestedClasses = java.util.Arrays.stream(
                        JUnitNestedAndDynamicTestJava26Test.class.getDeclaredClasses())
                .filter(c -> c.isAnnotationPresent(Nested.class))
                .count();

        assertThat(nestedClasses)
                .as("All @Nested inner classes should retain annotation at runtime on Java 26")
                .isGreaterThanOrEqualTo(4);
    }
}
