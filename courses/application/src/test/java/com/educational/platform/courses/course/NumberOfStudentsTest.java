package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberOfStudentsTest {

    @Test
    void create_validNumber_numberOfStudentsCreated() {
        // given
        final int number = 10;

        // when
        final NumberOfStudents numberOfStudents = new NumberOfStudents(number);

        // then
        assertThat(numberOfStudents.number()).isEqualTo(10);
    }

    @Test
    void equals_sameNumber_true() {
        // given
        final NumberOfStudents nos1 = new NumberOfStudents(5);
        final NumberOfStudents nos2 = new NumberOfStudents(5);

        // when / then
        assertThat(nos1).isEqualTo(nos2);
    }

    @Test
    void equals_differentNumber_false() {
        // given
        final NumberOfStudents nos1 = new NumberOfStudents(5);
        final NumberOfStudents nos2 = new NumberOfStudents(10);

        // when / then
        assertThat(nos1).isNotEqualTo(nos2);
    }
}
