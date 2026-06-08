package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that advanced pattern matching (record patterns, guarded patterns,
 * and exhaustive switch on sealed types) compiles and executes correctly
 * under Java 26.
 * <p>
 * Record patterns (JEP 440, finalized Java 21) and pattern matching for switch
 * (JEP 441, finalized Java 21) are exercised here with sealed types and
 * guarded conditions. This validates the Java 26 compiler correctly emits
 * bytecode for complex pattern combinations, ensuring ArchUnit and byte-buddy
 * can process the resulting class files.
 * <p>
 * The existing {@link Java26LanguageFeaturesTest} covers basic patterns and
 * switch expressions. This test exercises <em>advanced patterns</em>: nested
 * record destructuring, guarded patterns on sealed hierarchies, and
 * exhaustive switch on sealed types with primitive record components.
 */
public class Java26PrimitivePatternMatchingTest {

    // --- Record patterns with primitive components ---

    sealed interface VersionSpec permits MajorOnly, FullVersion {}
    record MajorOnly(int major) implements VersionSpec {}
    record FullVersion(int major, int minor, int patch) implements VersionSpec {}

    @Test
    void recordPattern_shouldDestructure_primitiveComponents() {
        VersionSpec gradle = new FullVersion(9, 5, 1);

        String description = switch (gradle) {
            case MajorOnly(var m) -> "Major " + m;
            case FullVersion(var maj, var min, var p) -> maj + "." + min + "." + p;
        };

        assertThat(description).isEqualTo("9.5.1");
    }

    @Test
    void recordPattern_shouldDestructure_majorOnly() {
        VersionSpec spec = new MajorOnly(26);

        String result = switch (spec) {
            case MajorOnly(var m) -> "Java " + m;
            case FullVersion(var maj, var min, var p) -> maj + "." + min + "." + p;
        };

        assertThat(result).isEqualTo("Java 26");
    }

    // --- Guarded patterns on sealed types ---

    @Test
    void guardedPattern_shouldFilter_onRecordComponents() {
        VersionSpec version = new FullVersion(9, 5, 1);

        boolean compatible = switch (version) {
            case FullVersion(var maj, var min, var p) when maj > 9 -> true;
            case FullVersion(var maj, var min, var p) when maj == 9 && min >= 4 -> true;
            case FullVersion f -> false;
            case MajorOnly(var m) -> m > 9;
        };

        assertThat(compatible)
                .as("Gradle 9.5.1 should be compatible with Java 26 (requires >= 9.4.0)")
                .isTrue();
    }

    @ParameterizedTest(name = "Gradle {0}.{1}.{2} Java 26 compatible via pattern matching: expected {3}")
    @CsvSource({
            "9, 5, 1, true",
            "9, 4, 0, true",
            "10, 0, 0, true",
            "9, 3, 9, false",
            "8, 9, 0, false"
    })
    void patternMatching_shouldEvaluate_compatibilityMatrix(
            int major, int minor, int patch, boolean expectedCompatible) {
        VersionSpec v = new FullVersion(major, minor, patch);

        boolean compatible = switch (v) {
            case FullVersion(var maj, var min, var p) when maj > 9 -> true;
            case FullVersion(var maj, var min, var p) when maj == 9 && min >= 4 -> true;
            case FullVersion f -> false;
            case MajorOnly(var m) -> m > 9;
        };

        assertThat(compatible).isEqualTo(expectedCompatible);
    }

    // --- Instanceof with guarded patterns ---

    @Test
    void instanceofPattern_shouldMatch_integerWrapper() {
        Object gradleMinor = Integer.valueOf(5);

        if (gradleMinor instanceof Integer minor && minor >= 4) {
            assertThat(minor)
                    .as("Gradle minor version should be >= 4 for Java 26 support")
                    .isGreaterThanOrEqualTo(4);
        } else {
            throw new AssertionError("Should match Integer pattern");
        }
    }

    @Test
    void instanceofPattern_shouldMatch_longWrapper() {
        Object bytecodeVersion = Long.valueOf(70L);

        if (bytecodeVersion instanceof Long version && version == 70L) {
            assertThat(version)
                    .as("Bytecode major version for Java 26")
                    .isEqualTo(70L);
        } else {
            throw new AssertionError("Should match Long pattern");
        }
    }

    // --- Complex pattern matching with multiple levels ---

    sealed interface Dependency permits TestDep, ProdDep {}
    record TestDep(String artifact, String scope) implements Dependency {}
    record ProdDep(String artifact) implements Dependency {}

    @Test
    void nestedSwitch_withPatterns_shouldClassifyDependencies() {
        Dependency dep = new TestDep("junit-jupiter-params", "testImplementation");

        String classification = switch (dep) {
            case TestDep(var art, var scope) when scope.contains("Runtime") -> "runtime-only";
            case TestDep(var art, var scope) when scope.contains("Implementation") -> "compile-time";
            case TestDep t -> "other-test";
            case ProdDep p -> "production";
        };

        assertThat(classification).isEqualTo("compile-time");
    }

    @ParameterizedTest(name = "Dependency {0} with scope {1} → {2}")
    @CsvSource({
            "junit-jupiter-params, testImplementation, compile-time",
            "assertj-core, testImplementation, compile-time",
            "junit-jupiter-engine, testRuntimeOnly, runtime-only",
            "spring-boot-starter-web, implementation, production"
    })
    void dependencyClassification_viaPatternMatching(
            String artifact, String scope, String expectedClass) {
        Dependency dep = scope.equals("implementation")
                ? new ProdDep(artifact)
                : new TestDep(artifact, scope);

        String classification = switch (dep) {
            case TestDep(var art, var s) when s.contains("Runtime") -> "runtime-only";
            case TestDep(var art, var s) when s.contains("Implementation") -> "compile-time";
            case TestDep t -> "other-test";
            case ProdDep p -> "production";
        };

        assertThat(classification).isEqualTo(expectedClass);
    }

    // --- Exhaustive switch on sealed type ---

    @Test
    void exhaustiveSwitch_onSealedType_shouldCompile_withJava26() {
        VersionSpec[] specs = {
                new MajorOnly(26),
                new FullVersion(9, 5, 1)
        };

        for (VersionSpec spec : specs) {
            String result = switch (spec) {
                case MajorOnly m -> "major=" + m.major();
                case FullVersion f -> "full=" + f.major() + "." + f.minor() + "." + f.patch();
            };
            assertThat(result).isNotBlank();
        }
    }

    // --- Pattern matching with class file version mapping ---

    @ParameterizedTest(name = "Major version {0} should map to Java {1}")
    @CsvSource({
            "70, 26",
            "69, 25",
            "65, 21",
            "61, 17",
            "55, 11",
            "52, 8"
    })
    void classFileMajorVersion_shouldMapTo_javaVersion(int majorVersion, int expectedJava) {
        int javaVersion = majorVersion >= 44 ? majorVersion - 44 : -1;

        assertThat(javaVersion).isEqualTo(expectedJava);
    }
}
