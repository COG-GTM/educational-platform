package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewWebRequestsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void reviewCourseRequest_validValues_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(4.0, "Good");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reviewCourseRequest_nullRating_hasViolation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(null, "Comment");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void reviewCourseRequest_negativeRating_hasViolation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(-1.0, "Bad");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void reviewCourseRequest_ratingAboveMax_hasViolation() {
        final ReviewCourseRequest request = new ReviewCourseRequest(6.0, "High");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void reviewCourseRequest_zeroRating_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(0.0, "Zero");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reviewCourseRequest_maxRating_noViolations() {
        final ReviewCourseRequest request = new ReviewCourseRequest(5.0, "Max");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateCourseReviewRequest_validValues_noViolations() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.5, "Updated");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateCourseReviewRequest_nullRating_hasViolation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(null, "Comment");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void updateCourseReviewRequest_negativeRating_hasViolation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(-0.5, "Bad");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void updateCourseReviewRequest_ratingAboveMax_hasViolation() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(5.1, "High");
        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void courseReviewCreatedResponse_exposesUuid() {
        final UUID uuid = UUID.randomUUID();
        final CourseReviewCreatedResponse response = new CourseReviewCreatedResponse(uuid);
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void courseReviewCreatedResponse_equalInstances() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CourseReviewCreatedResponse(uuid))
                .isEqualTo(new CourseReviewCreatedResponse(uuid));
    }

    @Test
    void reviewCourseRequest_accessors() {
        final ReviewCourseRequest request = new ReviewCourseRequest(4.5, "Nice");
        assertThat(request.rating()).isEqualTo(4.5);
        assertThat(request.comment()).isEqualTo("Nice");
    }

    @Test
    void updateCourseReviewRequest_accessors() {
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "Ok");
        assertThat(request.rating()).isEqualTo(3.0);
        assertThat(request.comment()).isEqualTo("Ok");
    }
}
