package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void create_validCommand_fieldsInitialized() {
        // given
        final ReviewCourseCommand command = new ReviewCourseCommand(COURSE_ID, 4.0, "comment");

        // when
        final CourseReview review = new CourseReview(command, 1, 2);

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("course", 1)
                .hasFieldOrPropertyWithValue("reviewer", 2)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"));
        assertThat(review.toIdentifier()).isNotNull();
    }

    @Test
    void update_validCommand_updatesRatingAndComment() {
        // given
        final CourseReview review = new CourseReview(new ReviewCourseCommand(COURSE_ID, 4.0, "comment"), 1, 2);

        // when
        review.update(new UpdateCourseReviewCommand(review.toIdentifier(), 2.0, "updated"));

        // then
        assertThat(review)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("updated"));
    }
}
