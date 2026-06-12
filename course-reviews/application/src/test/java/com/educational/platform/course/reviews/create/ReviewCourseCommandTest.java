package com.educational.platform.course.reviews.create;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewCourseCommandTest {

    @Test
    void constructor_allFieldsSet_fieldsAccessible() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ReviewCourseCommand sut = new ReviewCourseCommand(courseId, 4.5, "Great course");

        // then
        assertThat(sut.courseId()).isEqualTo(courseId);
        assertThat(sut.rating()).isEqualTo(4.5);
        assertThat(sut.comment()).isEqualTo("Great course");
    }

    @Test
    void constructor_nullComment_commentIsNull() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ReviewCourseCommand sut = new ReviewCourseCommand(courseId, 3.0, null);

        // then
        assertThat(sut.comment()).isNull();
    }

    @Test
    void constructor_zeroRating_ratingIsZero() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ReviewCourseCommand sut = new ReviewCourseCommand(courseId, 0.0, "comment");

        // then
        assertThat(sut.rating()).isZero();
    }

    @Test
    void constructor_maxRating_ratingIsFive() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ReviewCourseCommand sut = new ReviewCourseCommand(courseId, 5.0, "Excellent");

        // then
        assertThat(sut.rating()).isEqualTo(5.0);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand cmd1 = new ReviewCourseCommand(courseId, 4.0, "good");
        final ReviewCourseCommand cmd2 = new ReviewCourseCommand(courseId, 4.0, "good");

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentRating_notEqual() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand cmd1 = new ReviewCourseCommand(courseId, 4.0, "good");
        final ReviewCourseCommand cmd2 = new ReviewCourseCommand(courseId, 5.0, "good");

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
