package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewCreatedResponseTest {

    @Test
    void constructor_validUuid_uuidStored() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseReviewCreatedResponse response = new CourseReviewCreatedResponse(uuid);

        // then
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void constructor_nullUuid_nullStored() {
        // when
        final CourseReviewCreatedResponse response = new CourseReviewCreatedResponse(null);

        // then
        assertThat(response.uuid()).isNull();
    }

    @Test
    void equals_sameUuid_returnsTrue() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReviewCreatedResponse first = new CourseReviewCreatedResponse(uuid);
        final CourseReviewCreatedResponse second = new CourseReviewCreatedResponse(uuid);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentUuid_returnsFalse() {
        // given
        final CourseReviewCreatedResponse first = new CourseReviewCreatedResponse(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));
        final CourseReviewCreatedResponse second = new CourseReviewCreatedResponse(
                UUID.fromString("123e4567-e89b-12d3-a456-426655440002"));

        // then
        assertThat(first).isNotEqualTo(second);
    }
}
