package com.educational.platform.course.reviews.edit;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateCourseReviewCommandTest {

    @Test
    void constructor_allFieldsSet_fieldsAccessible() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseReviewCommand sut = new UpdateCourseReviewCommand(uuid, 3.5, "Updated comment");

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
        assertThat(sut.rating()).isEqualTo(3.5);
        assertThat(sut.comment()).isEqualTo("Updated comment");
    }

    @Test
    void constructor_nullComment_commentIsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseReviewCommand sut = new UpdateCourseReviewCommand(uuid, 4.0, null);

        // then
        assertThat(sut.comment()).isNull();
    }

    @Test
    void constructor_zeroRating_ratingIsZero() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseReviewCommand sut = new UpdateCourseReviewCommand(uuid, 0.0, "comment");

        // then
        assertThat(sut.rating()).isZero();
    }

    @Test
    void constructor_maxRating_ratingIsFive() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final UpdateCourseReviewCommand sut = new UpdateCourseReviewCommand(uuid, 5.0, "Perfect");

        // then
        assertThat(sut.rating()).isEqualTo(5.0);
    }

    @Test
    void equality_sameValues_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UpdateCourseReviewCommand cmd1 = new UpdateCourseReviewCommand(uuid, 4.0, "comment");
        final UpdateCourseReviewCommand cmd2 = new UpdateCourseReviewCommand(uuid, 4.0, "comment");

        // then
        assertThat(cmd1).isEqualTo(cmd2);
        assertThat(cmd1.hashCode()).isEqualTo(cmd2.hashCode());
    }

    @Test
    void equality_differentUuid_notEqual() {
        // given
        final UpdateCourseReviewCommand cmd1 = new UpdateCourseReviewCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0, "comment");
        final UpdateCourseReviewCommand cmd2 = new UpdateCourseReviewCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"), 4.0, "comment");

        // then
        assertThat(cmd1).isNotEqualTo(cmd2);
    }
}
