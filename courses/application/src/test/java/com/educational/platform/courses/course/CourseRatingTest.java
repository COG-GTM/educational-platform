package com.educational.platform.courses.course;

import com.educational.platform.common.domain.ValueObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRatingTest {

    @Test
    void constructor_positiveRating_storedCorrectly() {
        // when
        final CourseRating sut = new CourseRating(4.5);

        // then
        assertThat(sut.rating()).isEqualTo(4.5);
    }

    @Test
    void constructor_zero_storedCorrectly() {
        // when
        final CourseRating sut = new CourseRating(0.0);

        // then
        assertThat(sut.rating()).isEqualTo(0.0);
    }

    @Test
    void constructor_negativeRating_storedCorrectly() {
        // when
        final CourseRating sut = new CourseRating(-1.0);

        // then
        assertThat(sut.rating()).isEqualTo(-1.0);
    }

    @Test
    void constructor_maxDouble_storedCorrectly() {
        // when
        final CourseRating sut = new CourseRating(Double.MAX_VALUE);

        // then
        assertThat(sut.rating()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void constructor_fractionalPrecision_preserved() {
        // when
        final CourseRating sut = new CourseRating(3.14159);

        // then
        assertThat(sut.rating()).isEqualTo(3.14159);
    }

    @Test
    void implementsValueObject() {
        // then
        assertThat(ValueObject.class).isAssignableFrom(CourseRating.class);
    }

    @Test
    void equalInstances_areEqual() {
        // given
        final CourseRating a = new CourseRating(4.0);
        final CourseRating b = new CourseRating(4.0);

        // then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentInstances_areNotEqual() {
        // given
        final CourseRating a = new CourseRating(4.0);
        final CourseRating b = new CourseRating(5.0);

        // then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_containsRating() {
        // given
        final CourseRating sut = new CourseRating(3.7);

        // then
        assertThat(sut.toString()).contains("3.7");
    }
}
