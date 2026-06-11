package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseValueObjectsBoundaryTest {

    @Test
    void numberOfStudents_zero_allowed() {
        final NumberOfStudents nos = new NumberOfStudents(0);
        assertThat(nos.number()).isZero();
    }

    @Test
    void numberOfStudents_positiveValue_preserved() {
        final NumberOfStudents nos = new NumberOfStudents(42);
        assertThat(nos.number()).isEqualTo(42);
    }

    @Test
    void numberOfStudents_equalInstances() {
        assertThat(new NumberOfStudents(5)).isEqualTo(new NumberOfStudents(5));
    }

    @Test
    void numberOfStudents_differentValues_notEqual() {
        assertThat(new NumberOfStudents(1)).isNotEqualTo(new NumberOfStudents(2));
    }

    @Test
    void numberOfStudents_hashCodeConsistent() {
        assertThat(new NumberOfStudents(10).hashCode())
                .isEqualTo(new NumberOfStudents(10).hashCode());
    }

    @Test
    void courseRating_zeroValue() {
        final com.educational.platform.courses.course.CourseRating rating = new com.educational.platform.courses.course.CourseRating(0.0);
        assertThat(rating.rating()).isZero();
    }

    @Test
    void courseRating_maxValue() {
        final com.educational.platform.courses.course.CourseRating rating = new com.educational.platform.courses.course.CourseRating(5.0);
        assertThat(rating.rating()).isEqualTo(5.0);
    }

    @Test
    void courseRating_equalInstances() {
        assertThat(new com.educational.platform.courses.course.CourseRating(3.5))
                .isEqualTo(new com.educational.platform.courses.course.CourseRating(3.5));
    }

    @Test
    void courseRating_differentValues_notEqual() {
        assertThat(new com.educational.platform.courses.course.CourseRating(3.0))
                .isNotEqualTo(new com.educational.platform.courses.course.CourseRating(4.0));
    }

    @Test
    void courseLightDTO_numberOfStudentsConversion() {
        final CourseLightDTO dto = new CourseLightDTO(
                java.util.UUID.randomUUID(), "name", "desc", new NumberOfStudents(15));
        assertThat(dto.numberOfStudents()).isEqualTo(15);
    }

    @Test
    void courseLightDTO_equalInstances() {
        final java.util.UUID uuid = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        assertThat(new CourseLightDTO(uuid, "name", "desc", 3))
                .isEqualTo(new CourseLightDTO(uuid, "name", "desc", 3));
    }
}
