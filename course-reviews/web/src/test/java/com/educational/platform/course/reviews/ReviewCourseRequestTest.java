package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewCourseRequestTest {

    @Test
    void constructor_validArguments_requestCreated() {
        // when
        final ReviewCourseRequest sut = new ReviewCourseRequest(4.5, "Great course!");

        // then
        assertThat(sut.rating()).isEqualTo(4.5);
        assertThat(sut.comment()).isEqualTo("Great course!");
    }

    @Test
    void constructor_nullComment_requestCreated() {
        // when
        final ReviewCourseRequest sut = new ReviewCourseRequest(3.0, null);

        // then
        assertThat(sut.rating()).isEqualTo(3.0);
        assertThat(sut.comment()).isNull();
    }

    @Test
    void equals_sameValues_equal() {
        // given
        final ReviewCourseRequest request1 = new ReviewCourseRequest(4.0, "comment");
        final ReviewCourseRequest request2 = new ReviewCourseRequest(4.0, "comment");

        // when / then
        assertThat(request1).isEqualTo(request2);
    }
}
