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
}
