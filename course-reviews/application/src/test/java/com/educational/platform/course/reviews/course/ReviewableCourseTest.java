package com.educational.platform.course.reviews.course;

import com.educational.platform.course.reviews.course.create.CreateReviewableCourseCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewableCourseTest {

    @Test
    void constructor_validCommand_originalCourseIdSet() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        final ReviewableCourse reviewableCourse = new ReviewableCourse(command);

        // then
        assertThat(reviewableCourse)
                .hasFieldOrPropertyWithValue("originalCourseId", courseId);
    }

    @Test
    void getId_beforePersistence_returnsNull() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // when
        final ReviewableCourse reviewableCourse = new ReviewableCourse(command);

        // then
        assertThat(reviewableCourse.getId()).isNull();
    }

    @Test
    void constructor_nullUuid_originalCourseIdSetToNull() {
        // given
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(null);

        // when
        final ReviewableCourse reviewableCourse = new ReviewableCourse(command);

        // then
        assertThat(reviewableCourse)
                .hasFieldOrPropertyWithValue("originalCourseId", null);
    }

    @Test
    void constructor_twoDifferentCommands_differentOriginalCourseIds() {
        // given
        final UUID courseId1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID courseId2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final ReviewableCourse first = new ReviewableCourse(new CreateReviewableCourseCommand(courseId1));
        final ReviewableCourse second = new ReviewableCourse(new CreateReviewableCourseCommand(courseId2));

        // then
        assertThat(first).hasFieldOrPropertyWithValue("originalCourseId", courseId1);
        assertThat(second).hasFieldOrPropertyWithValue("originalCourseId", courseId2);
    }

    @Test
    void implementsAggregateRoot() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final ReviewableCourse reviewableCourse = new ReviewableCourse(new CreateReviewableCourseCommand(courseId));

        // then
        assertThat(reviewableCourse).isInstanceOf(com.educational.platform.common.domain.AggregateRoot.class);
    }
}
