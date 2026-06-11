package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseWebDTOsTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void createdCourseResponse_exposesUuid() {
        // when
        final CreatedCourseResponse response = new CreatedCourseResponse(UUID_VALUE);

        // then
        assertThat(response.uuid()).isEqualTo(UUID_VALUE);
    }

    @Test
    void createdCourseResponse_equalInstances() {
        assertThat(new CreatedCourseResponse(UUID_VALUE))
                .isEqualTo(new CreatedCourseResponse(UUID_VALUE));
    }

    @Test
    void createCourseRequest_exposesNameAndDescription() {
        // when
        final CreateCourseRequest request = new CreateCourseRequest("name", "description");

        // then
        assertThat(request.name()).isEqualTo("name");
        assertThat(request.description()).isEqualTo("description");
    }

    @Test
    void createCourseRequest_equalInstances() {
        assertThat(new CreateCourseRequest("name", "description"))
                .isEqualTo(new CreateCourseRequest("name", "description"));
    }
}
