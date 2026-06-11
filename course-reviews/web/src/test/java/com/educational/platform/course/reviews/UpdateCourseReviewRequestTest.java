package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateCourseReviewRequestTest {

    @Test
    void constructor_validArguments_requestCreated() {
        // when
        final UpdateCourseReviewRequest sut = new UpdateCourseReviewRequest(3.5, "updated comment");

        // then
        assertThat(sut.rating()).isEqualTo(3.5);
        assertThat(sut.comment()).isEqualTo("updated comment");
    }

    @Test
    void constructor_nullComment_requestCreated() {
        // when
        final UpdateCourseReviewRequest sut = new UpdateCourseReviewRequest(2.0, null);

        // then
        assertThat(sut.rating()).isEqualTo(2.0);
        assertThat(sut.comment()).isNull();
    }

    @Test
    void equals_sameValues_equal() {
        // given
        final UpdateCourseReviewRequest request1 = new UpdateCourseReviewRequest(4.0, "comment");
        final UpdateCourseReviewRequest request2 = new UpdateCourseReviewRequest(4.0, "comment");

        // when / then
        assertThat(request1).isEqualTo(request2);
    }
}
