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
        final CourseReview result = new CourseReview(command, 11, 22);

        // then
        assertThat(result)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(4.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("comment"))
                .hasFieldOrPropertyWithValue("course", 11)
                .hasFieldOrPropertyWithValue("reviewer", 22);
        assertThat(result.toIdentifier()).isNotNull();
    }

    @Test
    void update_validCommand_fieldsUpdated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand createCommand = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview sut = new CourseReview(createCommand, 11, 22);

        final UUID reviewUuid = sut.toIdentifier();
        final UpdateCourseReviewCommand updateCommand = new UpdateCourseReviewCommand(reviewUuid, 3.0, "updated comment");

        // when
        sut.update(updateCommand);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("rating", new CourseRating(3.0))
                .hasFieldOrPropertyWithValue("comment", new Comment("updated comment"));
    }

    @Test
    void toIdentifier_afterCreation_returnsUuid() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ReviewCourseCommand command = new ReviewCourseCommand(courseId, 4.0, "comment");
        final CourseReview sut = new CourseReview(command, 11, 22);

        // when
        final UUID identifier = sut.toIdentifier();

        // then
        assertThat(identifier).isNotNull();
    }
}
