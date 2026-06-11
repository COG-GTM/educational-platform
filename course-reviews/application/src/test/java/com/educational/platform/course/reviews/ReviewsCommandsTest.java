package com.educational.platform.course.reviews;

import com.educational.platform.course.reviews.create.ReviewCourseCommand;
import com.educational.platform.course.reviews.edit.UpdateCourseReviewCommand;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQuery;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewsCommandsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void reviewCourseCommand_exposesFields() {
        final ReviewCourseCommand command = new ReviewCourseCommand(UUID_VALUE, 4.0, "comment");
        assertThat(command.courseId()).isEqualTo(UUID_VALUE);
        assertThat(command.rating()).isEqualTo(4.0);
        assertThat(command.comment()).isEqualTo("comment");
    }

    @Test
    void updateCourseReviewCommand_exposesFields() {
        final UpdateCourseReviewCommand command = new UpdateCourseReviewCommand(UUID_VALUE, 2.0, "comment");
        assertThat(command.uuid()).isEqualTo(UUID_VALUE);
        assertThat(command.rating()).isEqualTo(2.0);
        assertThat(command.comment()).isEqualTo("comment");
    }

    @Test
    void listCourseReviewsByCourseUUIDQuery_exposesUuid() {
        assertThat(new ListCourseReviewsByCourseUUIDQuery(UUID_VALUE).uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void courseReviewDTO_exposesFields() {
        final CourseReviewDTO dto = new CourseReviewDTO(UUID_VALUE, UUID_VALUE, "username", "comment", 4.0);
        assertThat(dto.username()).isEqualTo("username");
        assertThat(dto.rating()).isEqualTo(4.0);
    }
}
