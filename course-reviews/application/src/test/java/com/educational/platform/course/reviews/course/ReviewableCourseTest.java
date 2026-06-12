package com.educational.platform.course.reviews.course;

import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewableCourseTest {

    @Test
    void constructor_validCommand_reviewableCourseCreated() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        final ReviewableCourse result = new ReviewableCourse(command);

        // then
        assertThat(result).hasFieldOrPropertyWithValue("originalCourseId", courseId);
    }

    @Test
    void getId_afterSettingId_returnsId() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);
        final ReviewableCourse sut = new ReviewableCourse(command);
        ReflectionTestUtils.setField(sut, "id", 42);

        // when
        final Integer id = sut.getId();

        // then
        assertThat(id).isEqualTo(42);
    }
}
