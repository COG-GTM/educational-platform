package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link NumberOfStudents} value object record semantics:
 * equality, hashCode, accessor.
 */
public class NumberOfStudentsValueObjectTest {

    @Test
    void number_returnsConstructorValue() {
        assertThat(new NumberOfStudents(5).number()).isEqualTo(5);
    }

    @Test
    void number_zero_returnsZero() {
        assertThat(new NumberOfStudents(0).number()).isZero();
    }

    @Test
    void equals_sameValue_areEqual() {
        assertThat(new NumberOfStudents(10)).isEqualTo(new NumberOfStudents(10));
    }

    @Test
    void equals_differentValue_notEqual() {
        assertThat(new NumberOfStudents(1)).isNotEqualTo(new NumberOfStudents(2));
    }

    @Test
    void hashCode_sameValue_sameHashCode() {
        assertThat(new NumberOfStudents(7).hashCode()).isEqualTo(new NumberOfStudents(7).hashCode());
    }
}
