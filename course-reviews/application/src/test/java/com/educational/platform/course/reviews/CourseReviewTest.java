package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewTest {

    @Test
    void constructor_validParameters_courseReviewCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        // when
        final CourseReview courseReview = new CourseReview(command, 11, 22);

        // then
        assertThat(courseReview.toIdentifier()).isNotNull();
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"))
                .hasFieldOrPropertyWithValue("course", 11)
                .hasFieldOrPropertyWithValue("reviewer", 22);
    }

    @Test
    void update_validCommand_fieldsUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3))
                .hasFieldOrPropertyWithValue("comment", new Comment("updated comment"));
    }

    @Test
    void toIdentifier_afterCreation_returnsUUID() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(command, 11, 22);

        // when
        final UUID identifier = courseReview.toIdentifier();

        // then
        assertThat(identifier).isNotNull();
    }

    @Test
    void constructor_twoDifferentInstances_uniqueUUIDs() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        // when
        final CourseReview first = new CourseReview(command, 11, 22);
        final CourseReview second = new CourseReview(command, 11, 22);

        // then
        assertThat(first.toIdentifier()).isNotEqualTo(second.toIdentifier());
    }

    @Test
    void update_validCommand_courseAndReviewerUnchanged() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 2.0, "new comment");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("course", 11)
                .hasFieldOrPropertyWithValue("reviewer", 22);
    }

    @Test
    void constructor_zeroRating_courseReviewCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 0.0, "comment");

        // when
        final CourseReview courseReview = new CourseReview(command, 11, 22);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void constructor_maxRating_courseReviewCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 5.0, "max");

        // when
        final CourseReview courseReview = new CourseReview(command, 11, 22);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void constructor_nullComment_courseReviewCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, null);

        // when
        final CourseReview courseReview = new CourseReview(command, 11, 22);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void update_validCommand_uuidPreserved() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID originalUuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(originalUuid, 2.0, "new");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview.toIdentifier()).isEqualTo(originalUuid);
    }

    @Test
    void constructor_emptyComment_courseReviewCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "");

        // when
        final CourseReview courseReview = new CourseReview(command, 11, 22);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("comment", new Comment(""));
    }

    @Test
    void update_sameValues_noChangeInFields() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 4.0, "comment");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
    }

    @Test
    void update_zeroRating_ratingUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 0.0, "zero");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(0.0));
    }

    @Test
    void update_maxRating_ratingUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 2.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 5.0, "max");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0));
    }

    @Test
    void update_nullComment_commentUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 3.0, null);

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void constructor_nullCourseAndReviewerIds_courseReviewCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        // when
        final CourseReview courseReview = new CourseReview(command, null, null);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("course", null)
                .hasFieldOrPropertyWithValue("reviewer", null);
        assertThat(courseReview.toIdentifier()).isNotNull();
    }

    @Test
    void update_emptyComment_commentUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 3.0, "");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("comment", new Comment(""));
    }

    @Test
    void update_fractionalRating_ratingUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(uuid, 3.7, "decent");

        // when
        courseReview.update(updateCommand);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3.7));
    }

    @Test
    void update_multipleUpdates_uuidPreservedThroughout() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "original");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID originalUuid = courseReview.toIdentifier();

        // when
        courseReview.update(new UpdateCourseReviewCommand(originalUuid, 1.0, "first update"));
        courseReview.update(new UpdateCourseReviewCommand(originalUuid, 5.0, "second update"));
        courseReview.update(new UpdateCourseReviewCommand(originalUuid, 3.0, "third update"));

        // then
        assertThat(courseReview.toIdentifier()).isEqualTo(originalUuid);
    }

    @Test
    void update_multipleUpdates_lastUpdateWins() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "original");
        final CourseReview courseReview = new CourseReview(createCommand, 11, 22);
        final UUID uuid = courseReview.toIdentifier();

        // when
        courseReview.update(new UpdateCourseReviewCommand(uuid, 1.0, "first update"));
        courseReview.update(new UpdateCourseReviewCommand(uuid, 5.0, "second update"));

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(5.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("second update"));
    }
}
