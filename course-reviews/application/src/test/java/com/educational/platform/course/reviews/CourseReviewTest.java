package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewTest {

    @Test
    void constructor_validCommand_courseReviewCreatedWithRatingAndComment() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");

        // when
        final CourseReview courseReview = new CourseReview(command, 1, 2);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("course", 1)
                .hasFieldOrPropertyWithValue("reviewer", 2)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
        assertThat(courseReview.toIdentifier()).isNotNull();
    }

    @Test
    void update_validCommand_ratingAndCommentUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReview courseReview = new CourseReview(new ReviewCourseCommand(courseId, 4.0, "comment"), 1, 2);
        final UUID uuid = courseReview.toIdentifier();
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(uuid, 3.0, "updated comment");

        // when
        courseReview.update(command);

        // then
        assertThat(courseReview)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3))
                .hasFieldOrPropertyWithValue("comment", new Comment("updated comment"));
        assertThat(courseReview.toIdentifier()).isEqualTo(uuid);
    }
}
