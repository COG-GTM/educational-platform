package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingTest {

    @Test
    void rating_returnsWrappedValue() {
        // given
        final CourseRating rating = new CourseRating(4.5);

        // then
        assertThat(rating.rating()).isEqualTo(4.5);
    }

    @Test
    void equals_sameRating_valueEquality() {
        // given - the rating-recalculated flow asserts the course's rating via value equality (new CourseRating(4.5)), so equality must be value-based
        final CourseRating first = new CourseRating(4.5);
        final CourseRating second = new CourseRating(4.5);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentRating_notEqual() {
        // given
        final CourseRating first = new CourseRating(4.5);
        final CourseRating second = new CourseRating(4.0);

        // then
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void create_zeroRating_initialRatingState() {
        // given - a freshly created course starts at a zero rating, the baseline before any review recalculation
        final CourseRating rating = new CourseRating(0.0);

        // then
        assertThat(rating.rating()).isEqualTo(0.0);
    }

    @Test
    void create_negativeRating_noValidation() {
        // given - the value object performs no range validation, mirroring the listener that forwards an out-of-range rating verbatim
        final CourseRating rating = new CourseRating(-1.0);

        // then
        assertThat(rating.rating()).isEqualTo(-1.0);
    }
}
