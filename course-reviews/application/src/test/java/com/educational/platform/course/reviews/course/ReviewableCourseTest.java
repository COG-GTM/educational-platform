package com.educational.platform.course.reviews.course;

import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewableCourseTest {

    @Test
    void constructor_fromCommand_originalCourseIdSet() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        final ReviewableCourse sut = new ReviewableCourse(command);

        // then
        assertThat(sut).hasFieldOrPropertyWithValue("originalCourseId", courseId);
    }

    @Test
    void getId_returnsIdentifier() {
        // given
        final ReviewableCourse sut = new ReviewableCourse(
                new CreateReviewableCourseCommand(UUID.fromString("123e4567-e89b-12d3-a456-426655440000")));
        ReflectionTestUtils.setField(sut, "id", 7);

        // when
        final Integer result = sut.getId();

        // then
        assertThat(result).isEqualTo(7);
    }
}
