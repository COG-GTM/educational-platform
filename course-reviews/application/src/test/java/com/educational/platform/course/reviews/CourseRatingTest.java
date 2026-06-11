package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingTest {

    @Test
    void constructor_validRating_courseRatingCreated() {
        // given
        final double rating = 4.5;

        // when
        final CourseRating result = new CourseRating(rating);

        // then
        assertThat(result.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameRating_true() {
        // given
        final CourseRating rating1 = new CourseRating(4.0);
        final CourseRating rating2 = new CourseRating(4.0);

        // when/then
        assertThat(rating1).isEqualTo(rating2);
    }

    @Test
    void equals_differentRating_false() {
        // given
        final CourseRating rating1 = new CourseRating(4.0);
        final CourseRating rating2 = new CourseRating(3.0);

        // when/then
        assertThat(rating1).isNotEqualTo(rating2);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, 1.0, 2.5, 3.0, 4.5, 5.0})
    void constructor_variousValidRatings_courseRatingCreated(double rating) {
        // when
        final CourseRating result = new CourseRating(rating);

        // then
        assertThat(result.rating()).isEqualTo(rating);
    }
}
