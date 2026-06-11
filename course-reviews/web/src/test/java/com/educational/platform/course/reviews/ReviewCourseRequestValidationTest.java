package com.educational.platform.course.reviews;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Bean Validation constraints on {@link ReviewCourseRequest}.
 * Constraints: rating is {@code @NotNull @PositiveOrZero @Max(5)}, comment is unconstrained.
 */
public class ReviewCourseRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(4.5, "Great");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nullRating_violation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(null, "comment");
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    void negativeRating_violation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(-1.0, "comment");
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    void ratingExceedsMax_violation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(5.1, "comment");
        final Set<ConstraintViolation<ReviewCourseRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    void zeroRating_valid() {
        final ReviewCourseRequest request = new ReviewCourseRequest(0.0, "comment");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void maxRating_valid() {
        final ReviewCourseRequest request = new ReviewCourseRequest(5.0, "comment");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nullComment_valid() {
        final ReviewCourseRequest request = new ReviewCourseRequest(3.0, null);
        assertThat(validator.validate(request)).isEmpty();
    }
}
