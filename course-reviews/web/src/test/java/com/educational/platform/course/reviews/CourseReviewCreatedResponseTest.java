package com.educational.platform.course.reviews;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CourseReviewCreatedResponse} record.
 */
public class CourseReviewCreatedResponseTest {

    @Test
    void uuid_returnsConstructedValue() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseReviewCreatedResponse response = new CourseReviewCreatedResponse(uuid);
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void equalInstances() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CourseReviewCreatedResponse(uuid))
                .isEqualTo(new CourseReviewCreatedResponse(uuid));
    }

    @Test
    void differentUuids_notEqual() {
        assertThat(new CourseReviewCreatedResponse(UUID.randomUUID()))
                .isNotEqualTo(new CourseReviewCreatedResponse(UUID.randomUUID()));
    }
}
