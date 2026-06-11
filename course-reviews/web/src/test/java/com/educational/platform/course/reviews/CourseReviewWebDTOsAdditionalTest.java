package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewWebDTOsAdditionalTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void courseReviewCreatedResponse_exposesUuid() {
        // when
        final CourseReviewCreatedResponse response = new CourseReviewCreatedResponse(UUID_VALUE);

        // then
        assertThat(response.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseReviewCreatedResponse_equalInstances() {
        assertThat(new CourseReviewCreatedResponse(UUID_VALUE))
                .isEqualTo(new CourseReviewCreatedResponse(UUID_VALUE));
    }

    @Test
    void reviewCourseRequest_exposesRatingAndComment() {
        // when
        final ReviewCourseRequest request = new ReviewCourseRequest(4.5, "great course");

        // then
        assertThat(request.rating()).isEqualTo(4.5);
        assertThat(request.comment()).isEqualTo("great course");
    }

    @Test
    void reviewCourseRequest_equalInstances() {
        assertThat(new ReviewCourseRequest(4.0, "comment"))
                .isEqualTo(new ReviewCourseRequest(4.0, "comment"));
    }

    @Test
    void reviewCourseRequest_differentRating_notEqual() {
        assertThat(new ReviewCourseRequest(4.0, "comment"))
                .isNotEqualTo(new ReviewCourseRequest(3.0, "comment"));
    }

    @Test
    void updateCourseReviewRequest_exposesRatingAndComment() {
        // when
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.5, "updated");

        // then
        assertThat(request.rating()).isEqualTo(3.5);
        assertThat(request.comment()).isEqualTo("updated");
    }

    @Test
    void updateCourseReviewRequest_equalInstances() {
        assertThat(new UpdateCourseReviewRequest(5.0, "excellent"))
                .isEqualTo(new UpdateCourseReviewRequest(5.0, "excellent"));
    }

    @Test
    void updateCourseReviewRequest_nullComment_allowed() {
        // when
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(2.0, null);

        // then
        assertThat(request.comment()).isNull();
        assertThat(request.rating()).isEqualTo(2.0);
    }
}
