package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests edge cases for comment field in {@link ReviewCourseRequest} and
 * {@link UpdateCourseReviewRequest}. The comment field has no validation
 * annotations, so null, empty, and whitespace-only values should all be valid.
 */
public class ReviewCourseRequestCommentEdgeCasesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void reviewCourseRequest_nullComment_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(3.0, null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reviewCourseRequest_emptyComment_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(3.0, "");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reviewCourseRequest_whitespaceOnlyComment_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(3.0, "   ");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reviewCourseRequest_exactlyMaxRating_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(5.0, "Perfect");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reviewCourseRequest_justAboveMax_hasViolation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(5.01, "Above max");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void updateCourseReviewRequest_nullComment_noViolations() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(2.5, null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateCourseReviewRequest_emptyComment_noViolations() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(2.5, "");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateCourseReviewRequest_exactlyMaxRating_noViolations() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(5.0, "Max");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateCourseReviewRequest_justAboveMax_hasViolation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(5.01, "Above");
        assertThat(validator.validate(request)).isNotEmpty();
    }
}
