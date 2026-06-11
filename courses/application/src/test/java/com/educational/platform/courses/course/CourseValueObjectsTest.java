package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseValueObjectsTest {

    @Test
    void numberOfStudents_exposesNumber() {
        // when
        final NumberOfStudents numberOfStudents = new NumberOfStudents(7);

        // then
        assertThat(numberOfStudents.number()).isEqualTo(7);
    }

    @Test
    void numberOfStudents_equalityBasedOnValue() {
        assertThat(new NumberOfStudents(3)).isEqualTo(new NumberOfStudents(3));
        assertThat(new NumberOfStudents(3)).isNotEqualTo(new NumberOfStudents(4));
    }

    @Test
    void courseRating_exposesRating() {
        // when
        final CourseRating rating = new CourseRating(4.5);

        // then
        assertThat(rating.rating()).isEqualTo(4.5);
    }

    @Test
    void courseRating_equalityBasedOnValue() {
        assertThat(new CourseRating(4.5)).isEqualTo(new CourseRating(4.5));
        assertThat(new CourseRating(4.5)).isNotEqualTo(new CourseRating(2.0));
    }
}
