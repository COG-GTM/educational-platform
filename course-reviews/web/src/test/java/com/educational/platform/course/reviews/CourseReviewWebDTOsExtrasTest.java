package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewWebDTOsExtrasTest {

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
        assertThat(new ReviewCourseRequest(4.5, "comment"))
                .isEqualTo(new ReviewCourseRequest(4.5, "comment"));
    }

    @Test
    void reviewCourseRequest_differentRating_notEqual() {
        assertThat(new ReviewCourseRequest(4.5, "comment"))
                .isNotEqualTo(new ReviewCourseRequest(3.0, "comment"));
    }

    @Test
    void updateCourseReviewRequest_exposesRatingAndComment() {
        // when
        final UpdateCourseReviewRequest request = new UpdateCourseReviewRequest(3.0, "updated");

        // then
        assertThat(request.rating()).isEqualTo(3.0);
        assertThat(request.comment()).isEqualTo("updated");
    }

    @Test
    void updateCourseReviewRequest_equalInstances() {
        assertThat(new UpdateCourseReviewRequest(3.0, "comment"))
                .isEqualTo(new UpdateCourseReviewRequest(3.0, "comment"));
    }

    @Test
    void courseReviewCreatedResponse_exposesUuid() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        final CourseReviewCreatedResponse response = new CourseReviewCreatedResponse(uuid);

        // then
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void courseReviewCreatedResponse_equalInstances() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CourseReviewCreatedResponse(uuid))
                .isEqualTo(new CourseReviewCreatedResponse(uuid));
    }
}
