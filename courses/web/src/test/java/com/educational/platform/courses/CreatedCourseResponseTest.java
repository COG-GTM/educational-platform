package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link CreatedCourseResponse} record.
 */
public class CreatedCourseResponseTest {

    @Test
    void uuid_returnsConstructedValue() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreatedCourseResponse response = new CreatedCourseResponse(uuid);
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void equalInstances() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CreatedCourseResponse(uuid)).isEqualTo(new CreatedCourseResponse(uuid));
    }

    @Test
    void differentUuids_notEqual() {
        assertThat(new CreatedCourseResponse(UUID.randomUUID()))
                .isNotEqualTo(new CreatedCourseResponse(UUID.randomUUID()));
    }
}
