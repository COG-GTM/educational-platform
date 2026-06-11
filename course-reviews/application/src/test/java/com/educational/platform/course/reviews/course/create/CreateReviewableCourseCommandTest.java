package com.educational.platform.course.reviews.course.create;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateReviewableCourseCommandTest {

    @Test
    void constructor_validUuid_uuidStored() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(courseId);

        // then
        assertThat(command.uuid()).isEqualTo(courseId);
    }

    @Test
    void constructor_nullUuid_nullStored() {
        // when
        final CreateReviewableCourseCommand command = new CreateReviewableCourseCommand(null);

        // then
        assertThat(command.uuid()).isNull();
    }

    @Test
    void equals_sameUuid_returnsTrue() {
        // given
        final UUID courseId = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateReviewableCourseCommand first = new CreateReviewableCourseCommand(courseId);
        final CreateReviewableCourseCommand second = new CreateReviewableCourseCommand(courseId);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentUuid_returnsFalse() {
        // given
        final CreateReviewableCourseCommand first = new CreateReviewableCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final CreateReviewableCourseCommand second = new CreateReviewableCourseCommand(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
