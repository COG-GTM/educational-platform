package com.educational.platform.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Validates that Java 26 bytecode correctly preserves sealed class/interface
 * attributes and that the reflection API exposes them correctly.
 * <p>
 * Sealed types (finalized in Java 17) use the {@code PermittedSubclasses}
 * class file attribute to restrict inheritance. Spring Framework 6+ uses
 * sealed type information for exhaustive pattern matching in bean creation
 * and for detecting permitted subtypes during component scanning.
 * <p>
 * {@link Java26LanguageFeaturesTest} verifies sealed types at the source level.
 * This test verifies that the <em>bytecode-level</em> metadata
 * ({@code getPermittedSubclasses()}, {@code isSealed()}) is correctly
 * preserved in Java 26 class file major version 70.
 */
public class Java26SealedClassBytecodeTest {

    // Define sealed types compiled with Java 26 for verification
    sealed interface UpgradeStatus permits Pending, Completed, Failed {}
    record Pending(String component) implements UpgradeStatus {}
    record Completed(String component, String version) implements UpgradeStatus {}
    record Failed(String component, String reason) implements UpgradeStatus {}

    sealed interface VersionConstraint permits MinimumVersion, ExactVersion, RangeVersion {}
    record MinimumVersion(int major, int minor, int patch) implements VersionConstraint {}
    record ExactVersion(int major, int minor, int patch) implements VersionConstraint {}
    record RangeVersion(int minMajor, int minMinor, int maxMajor, int maxMinor) implements VersionConstraint {}

    @Test
    void sealedInterface_shouldReport_isSealed() {
        assertThat(UpgradeStatus.class.isSealed())
                .as("UpgradeStatus interface should be sealed on Java 26")
                .isTrue();

        assertThat(VersionConstraint.class.isSealed())
                .as("VersionConstraint interface should be sealed on Java 26")
                .isTrue();
    }

    @Test
    void nonSealedClass_shouldReport_notSealed() {
        assertThat(String.class.isSealed())
                .as("String should not be sealed")
                .isFalse();

        assertThat(Object.class.isSealed())
                .as("Object should not be sealed")
                .isFalse();
    }

    @Test
    void sealedInterface_shouldList_permittedSubclasses() {
        Class<?>[] permitted = UpgradeStatus.class.getPermittedSubclasses();

        assertThat(permitted)
                .as("UpgradeStatus should have exactly 3 permitted subclasses")
                .hasSize(3)
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("Pending", "Completed", "Failed");
    }

    @Test
    void versionConstraint_shouldList_permittedSubclasses() {
        Class<?>[] permitted = VersionConstraint.class.getPermittedSubclasses();

        assertThat(permitted)
                .as("VersionConstraint should have exactly 3 permitted subclasses")
                .hasSize(3)
                .extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("MinimumVersion", "ExactVersion", "RangeVersion");
    }

    @Test
    void permittedSubclass_shouldBeRecord() {
        for (Class<?> subclass : UpgradeStatus.class.getPermittedSubclasses()) {
            assertThat(subclass.isRecord())
                    .as("%s should be a record", subclass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void permittedSubclass_shouldImplement_sealedParent() {
        for (Class<?> subclass : UpgradeStatus.class.getPermittedSubclasses()) {
            assertThat(UpgradeStatus.class.isAssignableFrom(subclass))
                    .as("%s should implement UpgradeStatus", subclass.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void sealedInterface_shouldBe_interface() {
        assertThat(UpgradeStatus.class.isInterface())
                .as("Sealed interface should still report as interface on Java 26")
                .isTrue();
    }

    @Test
    void exhaustivePatternMatching_shouldWork_withSealedTypes() {
        UpgradeStatus status = new Completed("ArchUnit", "1.4.2");

        String description = switch (status) {
            case Pending p -> "pending: " + p.component();
            case Completed c -> c.component() + " " + c.version();
            case Failed f -> "failed: " + f.reason();
        };

        assertThat(description)
                .as("Pattern matching with sealed type should work on Java 26")
                .isEqualTo("ArchUnit 1.4.2");
    }

    @Test
    void sealedType_reflection_shouldNotThrow() {
        assertThatCode(() -> {
            Class<?> sealedClass = UpgradeStatus.class;
            boolean sealed = sealedClass.isSealed();
            Class<?>[] permitted = sealedClass.getPermittedSubclasses();
            int modifiers = sealedClass.getModifiers();

            assertThat(sealed).isTrue();
            assertThat(permitted).isNotEmpty();
            assertThat(Modifier.isAbstract(modifiers)).isTrue(); // interfaces are abstract
        }).as("Sealed type reflection APIs should not throw on Java 26")
                .doesNotThrowAnyException();
    }

    static Stream<Arguments> sealedPermitPairs() {
        return Stream.of(
                Arguments.of(UpgradeStatus.class, Pending.class),
                Arguments.of(UpgradeStatus.class, Completed.class),
                Arguments.of(UpgradeStatus.class, Failed.class),
                Arguments.of(VersionConstraint.class, MinimumVersion.class),
                Arguments.of(VersionConstraint.class, ExactVersion.class),
                Arguments.of(VersionConstraint.class, RangeVersion.class)
        );
    }

    @ParameterizedTest(name = "{0} should permit {1}")
    @MethodSource("sealedPermitPairs")
    void sealedParent_shouldPermit_specificSubclass(Class<?> parent, Class<?> child) {
        Class<?>[] permitted = parent.getPermittedSubclasses();

        assertThat(Arrays.asList(permitted))
                .as("%s should be in permitted subclasses of %s",
                        child.getSimpleName(), parent.getSimpleName())
                .contains(child);
    }
}
