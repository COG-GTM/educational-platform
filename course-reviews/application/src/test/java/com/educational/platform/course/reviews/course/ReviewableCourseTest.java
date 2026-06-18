package com.educational.platform.course.reviews.course;

import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewableCourseTest {

    @Test
    void create_validCommand_courseCreatedWithOriginalCourseId() {
        // given - the reviewable course is the reviews-context projection of a course keyed by the shared uuid
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(uuid);

        // when
        final ReviewableCourse course = new ReviewableCourse(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("originalCourseId", uuid);
        assertThat(course.getId()).isNull();
    }

    @Test
    void create_nullUuid_originalCourseIdIsNull() {
        // given - a null uuid is forwarded verbatim, the domain does not reject it
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(null);

        // when
        final ReviewableCourse course = new ReviewableCourse(command);

        // then
        assertThat(course)
                .hasFieldOrPropertyWithValue("originalCourseId", null);
    }
}
