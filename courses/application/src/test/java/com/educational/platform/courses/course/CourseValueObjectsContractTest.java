package com.educational.platform.courses.course;

import com.educational.platform.common.domain.ValueObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies contract-level behaviour of value objects in the courses module:
 * {@link NumberOfStudents} and {@link CourseRating} must implement
 * {@link ValueObject}, maintain hashCode/equals consistency, and accept
 * boundary values that the domain does not guard against (negative numbers).
 */
public class CourseValueObjectsContractTest {

    // --- NumberOfStudents ---

    @Test
    void numberOfStudents_implementsValueObject() {
        assertThat(new NumberOfStudents(0)).isInstanceOf(ValueObject.class);
    }

    @Test
    void numberOfStudents_negativeValue_accepted() {
        // domain does not guard against negative; verify it stores the value
        final NumberOfStudents nos = new NumberOfStudents(-1);
        assertThat(nos.number()).isEqualTo(-1);
    }

    @Test
    void numberOfStudents_largeValue_accepted() {
        final NumberOfStudents nos = new NumberOfStudents(Integer.MAX_VALUE);
        assertThat(nos.number()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void numberOfStudents_toStringContainsValue() {
        assertThat(new NumberOfStudents(42).toString()).contains("42");
    }

    // --- CourseRating ---

    @Test
    void courseRating_implementsValueObject() {
        assertThat(new CourseRating(0.0)).isInstanceOf(ValueObject.class);
    }

    @Test
    void courseRating_negativeValue_accepted() {
        // domain does not guard against negative; verify it stores the value
        final CourseRating rating = new CourseRating(-1.5);
        assertThat(rating.rating()).isEqualTo(-1.5);
    }

    @Test
    void courseRating_veryLargeValue_accepted() {
        final CourseRating rating = new CourseRating(Double.MAX_VALUE);
        assertThat(rating.rating()).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void courseRating_hashCodeConsistentWithEquals() {
        final CourseRating first = new CourseRating(3.5);
        final CourseRating second = new CourseRating(3.5);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void courseRating_toStringContainsValue() {
        assertThat(new CourseRating(4.5).toString()).contains("4.5");
    }
}
