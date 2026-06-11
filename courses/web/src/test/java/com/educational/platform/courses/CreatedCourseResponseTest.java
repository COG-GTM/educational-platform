package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreatedCourseResponseTest {

    @Test
    void constructor_validUuid_responseCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CreatedCourseResponse sut = new CreatedCourseResponse(uuid);

        // then
        assertThat(sut.uuid()).isEqualTo(uuid);
    }

    @Test
    void constructor_nullUuid_responseCreated() {
        // when
        final CreatedCourseResponse sut = new CreatedCourseResponse(null);

        // then
        assertThat(sut.uuid()).isNull();
    }

    @Test
    void equals_sameUuid_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreatedCourseResponse response1 = new CreatedCourseResponse(uuid);
        final CreatedCourseResponse response2 = new CreatedCourseResponse(uuid);

        // when / then
        assertThat(response1).isEqualTo(response2);
    }
}
