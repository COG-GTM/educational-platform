package com.educational.platform.courses.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CourseRating} value object record semantics:
 * equality, hashCode, accessor, and boundary values.
 */
public class CourseRatingValueObjectTest {

    @Test
    void rating_returnsConstructorValue() {
        assertThat(new CourseRating(4.5).rating()).isEqualTo(4.5);
    }

    @Test
    void rating_zero_returnsZero() {
        assertThat(new CourseRating(0).rating()).isZero();
    }

    @Test
    void equals_sameValue_areEqual() {
        assertThat(new CourseRating(3.5)).isEqualTo(new CourseRating(3.5));
    }

    @Test
    void equals_differentValue_notEqual() {
        assertThat(new CourseRating(3.5)).isNotEqualTo(new CourseRating(4.0));
    }

    @Test
    void hashCode_sameValue_sameHashCode() {
        assertThat(new CourseRating(2.0).hashCode()).isEqualTo(new CourseRating(2.0).hashCode());
    }

    @Test
    void rating_maxBoundary_returnsCorrectValue() {
        assertThat(new CourseRating(5.0).rating()).isEqualTo(5.0);
    }
}
