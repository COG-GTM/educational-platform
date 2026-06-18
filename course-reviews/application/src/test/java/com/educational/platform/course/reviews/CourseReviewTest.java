package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewTest {

    @Test
    void create_validCommand_ratingCommentAndReferencesMapped() {
        // given - the aggregate copies the rating/comment from the command and keeps the resolved course/reviewer references
        final ReviewCourseCommand command = new ReviewCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0, "great course");

        // when
        final CourseReview review = new CourseReview(command, 11, 22);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("great course"))
                .hasFieldOrPropertyWithValue("course", 11)
                .hasFieldOrPropertyWithValue("reviewer", 22);
        assertThat(review.toIdentifier()).isNotNull();
    }

    @Test
    void create_nullComment_commentMappedVerbatim() {
        // given - the domain performs no validation; a null comment is wrapped as-is (validation lives in the factory)
        final ReviewCourseCommand command = new ReviewCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0, null);

        // when
        final CourseReview review = new CourseReview(command, 11, 22);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("comment", new Comment(null));
    }

    @Test
    void create_distinctReviews_haveDistinctIdentifiers() {
        // given - the uuid is the natural key generated per aggregate, so two reviews never share an identifier
        final ReviewCourseCommand command = new ReviewCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0, "comment");

        // when
        final CourseReview first = new CourseReview(command, 11, 22);
        final CourseReview second = new CourseReview(command, 11, 22);

        // then
        assertThat(first.toIdentifier())
                .isNotNull()
                .isNotEqualTo(second.toIdentifier());
    }

    @Test
    void update_overwritesRatingAndComment() {
        // given - update replaces the prior rating/comment rather than merging, mirroring the recalculation overwrite contract
        final CourseReview review = new CourseReview(
                new ReviewCourseCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0, "great course"),
                11, 22);
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(
                review.toIdentifier(), 2.0, "could be better");

        // when
        review.update(command);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("could be better"));
    }

    @Test
    void update_doesNotChangeIdentifier() {
        // given - update mutates only the rating/comment, never the natural key used to load and persist the aggregate
        final CourseReview review = new CourseReview(
                new ReviewCourseCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"), 4.0, "great course"),
                11, 22);
        final UUID identifierBeforeUpdate = review.toIdentifier();

        // when
        review.update(new UpdateCourseReviewCommand(identifierBeforeUpdate, 2.0, "could be better"));

        // then
        assertThat(review.toIdentifier()).isEqualTo(identifierBeforeUpdate);
    }
}
