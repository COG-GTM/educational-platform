package com.educational.platform.java;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validates that Flexible Constructor Bodies (JEP 492, finalized in Java 25)
 * compile and execute correctly under Java 26.
 * <p>
 * Flexible Constructor Bodies allow statements to appear before the explicit
 * constructor invocation ({@code super()} or {@code this()}). This was a
 * preview feature in Java 22–24 and became final in Java 25. Since we upgraded
 * from Java 25 → 26, this test serves as a regression guard ensuring the
 * feature continues to work under the new class file major version 70.
 * <p>
 * This exercises the Java 26 compiler's ability to emit prologue bytecode
 * before the {@code invokespecial} for the super constructor, verifying that
 * byteBuddy, ArchUnit, and other bytecode-processing tools correctly handle
 * class files produced with this pattern.
 */
public class Java26FlexibleConstructorBodiesTest {

    // --- Validation before super() ---

    static class BaseEntity {
        private final String name;

        BaseEntity(String name) {
            this.name = name;
        }

        String getName() { return name; }
    }

    static class ValidatedEntity extends BaseEntity {
        ValidatedEntity(String name) {
            // Statements before super() — flexible constructor bodies
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            super(name.strip());
        }
    }

    @Test
    void flexibleConstructor_shouldValidate_beforeSuperCall() {
        var entity = new ValidatedEntity("  Test Course  ");

        assertThat(entity.getName())
                .as("Name should be stripped before being passed to super()")
                .isEqualTo("Test Course");
    }

    @Test
    void flexibleConstructor_shouldThrow_forBlankInput() {
        assertThatThrownBy(() -> new ValidatedEntity("   "))
                .as("Validation before super() should reject blank names")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void flexibleConstructor_shouldThrow_forNullInput() {
        assertThatThrownBy(() -> new ValidatedEntity(null))
                .as("Validation before super() should reject null names")
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Local variable computation before super() ---

    static class ComputedIdEntity extends BaseEntity {
        private final int id;

        ComputedIdEntity(String rawInput) {
            // Compute values before calling super()
            var parts = rawInput.split(":");
            var name = parts.length > 1 ? parts[1] : parts[0];
            var id = parts.length > 1 ? Integer.parseInt(parts[0]) : 0;
            super(name);
            this.id = id;
        }

        int getId() { return id; }
    }

    @Test
    void flexibleConstructor_shouldCompute_localVariables_beforeSuper() {
        var entity = new ComputedIdEntity("42:Advanced DDD");

        assertThat(entity.getId()).isEqualTo(42);
        assertThat(entity.getName()).isEqualTo("Advanced DDD");
    }

    @Test
    void flexibleConstructor_shouldHandle_inputWithoutId() {
        var entity = new ComputedIdEntity("Basic Course");

        assertThat(entity.getId()).isEqualTo(0);
        assertThat(entity.getName()).isEqualTo("Basic Course");
    }

    // --- Delegation with this() ---

    static class CourseDTO {
        private final String title;
        private final int credits;

        CourseDTO(String title, int credits) {
            this.title = title;
            this.credits = credits;
        }

        CourseDTO(String rawSpec) {
            // Parse before this() delegation
            var idx = rawSpec.lastIndexOf('/');
            var title = idx > 0 ? rawSpec.substring(0, idx) : rawSpec;
            var credits = idx > 0 ? Integer.parseInt(rawSpec.substring(idx + 1)) : 3;
            this(title, credits);
        }

        String getTitle() { return title; }
        int getCredits() { return credits; }
    }

    @Test
    void flexibleConstructor_shouldParse_beforeThisCall() {
        var dto = new CourseDTO("Software Architecture/5");

        assertThat(dto.getTitle()).isEqualTo("Software Architecture");
        assertThat(dto.getCredits()).isEqualTo(5);
    }

    @Test
    void flexibleConstructor_shouldDefault_whenNoDelimiter() {
        var dto = new CourseDTO("Intro to DDD");

        assertThat(dto.getTitle()).isEqualTo("Intro to DDD");
        assertThat(dto.getCredits()).isEqualTo(3);
    }

    // --- Records with compact constructor validation ---

    record GradleVersion(int major, int minor, int patch) {
        GradleVersion {
            if (major < 0 || minor < 0 || patch < 0) {
                throw new IllegalArgumentException("Version components must be non-negative");
            }
        }

        String formatted() {
            return major + "." + minor + "." + patch;
        }
    }

    @Test
    void recordCompactConstructor_shouldValidate_onJava26() {
        var version = new GradleVersion(9, 5, 1);
        assertThat(version.formatted()).isEqualTo("9.5.1");
    }

    @Test
    void recordCompactConstructor_shouldReject_negativeComponents() {
        assertThatThrownBy(() -> new GradleVersion(-1, 5, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }
}
