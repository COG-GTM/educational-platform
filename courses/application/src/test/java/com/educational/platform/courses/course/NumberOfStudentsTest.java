package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberOfStudentsTest {

    @Test
    void constructor_value_numberOfStudentsCreated() {
        // given
        final int value = 5;

        // when
        final NumberOfStudents numberOfStudents = new NumberOfStudents(value);

        // then
        assertThat(numberOfStudents.number()).isEqualTo(5);
    }

    @Test
    void equals_sameValue_equal() {
        // given
        final NumberOfStudents numberOfStudents = new NumberOfStudents(5);

        // when
        final NumberOfStudents same = new NumberOfStudents(5);

        // then
        assertThat(numberOfStudents).isEqualTo(same);
        assertThat(numberOfStudents).isNotEqualTo(new NumberOfStudents(6));
    }
}
