package com.educational.platform.course.reviews;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the Bean Validation constraints on {@link UpdateCourseReviewRequest}.
 * Constraints: rating is {@code @NotNull @PositiveOrZero @Max(5)}, comment is unconstrained.
 */
public class UpdateCourseReviewRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_noViolations() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "Updated review");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void nullRating_violation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(null, "comment");
        final Set<ConstraintViolation<UpdateCourseReviewRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    void negativeRating_violation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(-0.5, "comment");
        final Set<ConstraintViolation<UpdateCourseReviewRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    void ratingExceedsMax_violation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(6.0, "comment");
        final Set<ConstraintViolation<UpdateCourseReviewRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rating"));
    }

    @Test
    void boundaryZeroRating_valid() {
        assertThat(validator.validate(new UpdateCourseReviewRequest(0.0, "ok"))).isEmpty();
    }

    @Test
    void boundaryMaxRating_valid() {
        assertThat(validator.validate(new UpdateCourseReviewRequest(5.0, "ok"))).isEmpty();
    }

    @Test
    void nullComment_valid() {
        assertThat(validator.validate(new UpdateCourseReviewRequest(2.0, null))).isEmpty();
    }
}
