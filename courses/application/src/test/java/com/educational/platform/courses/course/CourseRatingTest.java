package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingTest {

    @Test
    void create_validRating_courseRatingCreated() {
        // given
        final double ratingValue = 4.5;

        // when
        final CourseRating courseRating = new CourseRating(ratingValue);

        // then
        assertThat(courseRating.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameRating_true() {
        // given
        final CourseRating rating1 = new CourseRating(3.2);
        final CourseRating rating2 = new CourseRating(3.2);

        // when / then
        assertThat(rating1).isEqualTo(rating2);
    }

    @Test
    void equals_differentRating_false() {
        // given
        final CourseRating rating1 = new CourseRating(3.2);
        final CourseRating rating2 = new CourseRating(4.5);

        // when / then
        assertThat(rating1).isNotEqualTo(rating2);
    }
}
