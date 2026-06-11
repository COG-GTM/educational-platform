package com.educational.platform.course.reviews;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewCourseRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void valid_noViolations() {
        // given
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "great course");

        // when
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullRating_violation() {
        // given
        final ReviewCourseRequest request = new ReviewCourseRequest(null, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, -0.1, 6, 5.1})
    void invalidRating_violation(double rating) {
        // given
        final ReviewCourseRequest request = new ReviewCourseRequest(rating, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 2.5, 5.0})
    void validRatingBoundaries_noViolations(double rating) {
        // given
        final ReviewCourseRequest request = new ReviewCourseRequest(rating, "comment");

        // when
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void nullComment_noViolations() {
        // given
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, null);

        // when
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    void accessors_returnProvidedValues() {
        // when
        final ReviewCourseRequest request = new ReviewCourseRequest(3.5, "nice");

        // then
        assertThat(request.rating()).isEqualTo(3.5);
        assertThat(request.comment()).isEqualTo("nice");
    }

    @Test
    void equals_sameValues_returnsTrue() {
        // given
        final ReviewCourseRequest first = new ReviewCourseRequest(4.0, "good");
        final ReviewCourseRequest second = new ReviewCourseRequest(4.0, "good");

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentValues_returnsFalse() {
        // given
        final ReviewCourseRequest first = new ReviewCourseRequest(4.0, "good");
        final ReviewCourseRequest second = new ReviewCourseRequest(5.0, "great");

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
