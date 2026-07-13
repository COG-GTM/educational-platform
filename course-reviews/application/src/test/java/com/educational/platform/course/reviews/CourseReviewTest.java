package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewTest {

    private static final Integer COURSE_ID = 11;
    private static final Integer REVIEWER_ID = 22;

    @Test
    void constructor_fromCommand_fieldsPopulated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "great course");

        // when
        final CourseReview sut = new CourseReview(command, COURSE_ID, REVIEWER_ID);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("course", COURSE_ID)
                .hasFieldOrPropertyWithValue("reviewer", REVIEWER_ID)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("great course"));
        assertThat(sut.toIdentifier()).isNotNull();
    }

    @Test
    void update_newValues_ratingAndCommentUpdatedIdentifierUnchanged() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        final CourseReview sut = new CourseReview(new ReviewCourseCommand(courseId, 4.0, "old comment"), COURSE_ID, REVIEWER_ID);
        final UUID identifier = sut.toIdentifier();

        // when
        sut.update(new UpdateCourseReviewCommand(identifier, 2.0, "new comment"));

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(2.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("new comment"));
        assertThat(sut.toIdentifier()).isEqualTo(identifier);
    }
}
