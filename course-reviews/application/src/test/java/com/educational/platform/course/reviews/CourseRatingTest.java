package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingTest {

    @Test
    void constructor_validRating_ratingStored() {
        // when
        final CourseRating courseRating = new CourseRating(4.5);

        // then
        assertThat(courseRating.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameRating_returnsTrue() {
        // given
        final CourseRating first = new CourseRating(3.0);
        final CourseRating second = new CourseRating(3.0);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentRating_returnsFalse() {
        // given
        final CourseRating first = new CourseRating(3.0);
        final CourseRating second = new CourseRating(4.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void constructor_zeroRating_ratingStored() {
        // when
        final CourseRating courseRating = new CourseRating(0.0);

        // then
        assertThat(courseRating.rating()).isEqualTo(0.0);
    }

    @Test
    void constructor_maxRating_ratingStored() {
        // when
        final CourseRating courseRating = new CourseRating(5.0);

        // then
        assertThat(courseRating.rating()).isEqualTo(5.0);
    }

    @Test
    void constructor_negativeRating_ratingStored() {
        // when — value object stores any value; validation is at the command level
        final CourseRating courseRating = new CourseRating(-1.0);

        // then
        assertThat(courseRating.rating()).isEqualTo(-1.0);
    }

    @Test
    void constructor_fractionalRating_ratingStored() {
        // when
        final CourseRating courseRating = new CourseRating(3.7);

        // then
        assertThat(courseRating.rating()).isEqualTo(3.7);
    }
}
