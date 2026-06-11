package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseReviewCreatedResponseTest {

    @Test
    void constructor_validUuid_responseCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseReviewCreatedResponse sut = new CourseReviewCreatedResponse(uuid);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
    }

    @Test
    void constructor_nullUuid_responseCreated() {
        // when
        final CourseReviewCreatedResponse sut = new CourseReviewCreatedResponse(null);

        // then
        assertThat(sut.uuid()).isNull();
    }

    @Test
    void equals_sameUuid_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReviewCreatedResponse response1 = new CourseReviewCreatedResponse(uuid);
        final CourseReviewCreatedResponse response2 = new CourseReviewCreatedResponse(uuid);

        // when / then
        assertThat(response1).isEqualTo(response2);
    }
}
