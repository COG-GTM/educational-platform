package com.educational.platform.courses.course;

import com.educational.platform.common.domain.ValueObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link NumberOfStudents} and {@link CourseRating} value objects
 * for equality, boundary values, and interface conformance.
 */
public class CourseValueObjectsTest {

    // --- NumberOfStudents ---

    @Test
    void numberOfStudents_zeroValue() {
        final NumberOfStudents nos = new NumberOfStudents(0);
        assertThat(nos.number()).isZero();
    }

    @Test
    void numberOfStudents_positiveValue() {
        final NumberOfStudents nos = new NumberOfStudents(100);
        assertThat(nos.number()).isEqualTo(100);
    }

    @Test
    void numberOfStudents_equalInstances() {
        assertThat(new NumberOfStudents(5)).isEqualTo(new NumberOfStudents(5));
    }

    @Test
    void numberOfStudents_differentValues_notEqual() {
        assertThat(new NumberOfStudents(5)).isNotEqualTo(new NumberOfStudents(6));
    }

    @Test
    void numberOfStudents_implementsValueObject() {
        assertThat(new NumberOfStudents(1)).isInstanceOf(ValueObject.class);
    }

    // --- CourseRating ---

    @Test
    void courseRating_zeroValue() {
        final CourseRating rating = new CourseRating(0.0);
        assertThat(rating.rating()).isZero();
    }

    @Test
    void courseRating_maxBoundary() {
        final CourseRating rating = new CourseRating(5.0);
        assertThat(rating.rating()).isEqualTo(5.0);
    }

    @Test
    void courseRating_fractionalValue() {
        final CourseRating rating = new CourseRating(3.7);
        assertThat(rating.rating()).isEqualTo(3.7);
    }

    @Test
    void courseRating_equalInstances() {
        assertThat(new CourseRating(4.5)).isEqualTo(new CourseRating(4.5));
    }

    @Test
    void courseRating_differentValues_notEqual() {
        assertThat(new CourseRating(4.5)).isNotEqualTo(new CourseRating(4.6));
    }

    @Test
    void courseRating_implementsValueObject() {
        assertThat(new CourseRating(0)).isInstanceOf(ValueObject.class);
    }

    @Test
    void courseRating_negativeValue_allowed() {
        final CourseRating rating = new CourseRating(-1.0);
        assertThat(rating.rating()).isEqualTo(-1.0);
    }
}
