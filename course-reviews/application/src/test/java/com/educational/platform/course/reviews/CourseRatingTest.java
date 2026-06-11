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

    @Test
    void equals_nanValues_recordSemanticsReturnsTrue() {
        // Records compare double fields using Double.equals(), where NaN == NaN.
        // This differs from primitive == semantics. Important for correctness of sets/maps.
        final CourseRating first = new CourseRating(Double.NaN);
        final CourseRating second = new CourseRating(Double.NaN);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_positiveAndNegativeZero_returnsFalse() {
        // +0.0 and -0.0 are distinct per Double.equals() in records
        final CourseRating positiveZero = new CourseRating(+0.0);
        final CourseRating negativeZero = new CourseRating(-0.0);

        // then — record uses Double.equals where +0.0 != -0.0
        assertThat(positiveZero).isNotEqualTo(negativeZero);
    }

    @Test
    void constructor_positiveInfinity_ratingStored() {
        // when — value object stores any value; validation is at the command level
        final CourseRating courseRating = new CourseRating(Double.POSITIVE_INFINITY);

        // then
        assertThat(courseRating.rating()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void implementsValueObject() {
        // then
        assertThat(new CourseRating(4.0)).isInstanceOf(com.educational.platform.common.domain.ValueObject.class);
    }
}
