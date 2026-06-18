package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberOfStudentsTest {

    @Test
    void number_returnsWrappedValue() {
        // given
        final NumberOfStudents numberOfStudents = new NumberOfStudents(3);

        // then
        assertThat(numberOfStudents.number()).isEqualTo(3);
    }

    @Test
    void equals_sameNumber_valueEquality() {
        // given - the enrollment flow asserts the course's numberOfStudents via value equality (new NumberOfStudents(1)), so equality must be value-based
        final NumberOfStudents first = new NumberOfStudents(1);
        final NumberOfStudents second = new NumberOfStudents(1);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentNumber_notEqual() {
        // given
        final NumberOfStudents first = new NumberOfStudents(1);
        final NumberOfStudents second = new NumberOfStudents(2);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void create_zero_initialEnrollmentState() {
        // given - a freshly created course starts at zero students, the baseline the enrollment flow increments from
        final NumberOfStudents numberOfStudents = new NumberOfStudents(0);

        // then
        assertThat(numberOfStudents.number()).isEqualTo(0);
    }

    @Test
    void create_negativeNumber_noValidation() {
        // given - the value object performs no range validation; any int is wrapped verbatim
        final NumberOfStudents numberOfStudents = new NumberOfStudents(-1);

        // then
        assertThat(numberOfStudents.number()).isEqualTo(-1);
    }
}
