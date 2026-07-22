package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingTest {

    @Test
    void constructor_value_ratingCreated() {
        // given
        final double value = 4.5;

        // when
        final CourseRating rating = new CourseRating(value);

        // then
        assertThat(rating.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameValue_equal() {
        // given
        final CourseRating rating = new CourseRating(4.5);

        // when
        final CourseRating same = new CourseRating(4.5);

        // then
        assertThat(rating).isEqualTo(same);
        assertThat(rating).isNotEqualTo(new CourseRating(3.5));
    }
}
