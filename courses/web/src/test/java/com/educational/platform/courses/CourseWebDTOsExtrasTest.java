package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseWebDTOsExtrasTest {

    @Test
    void createCourseRequest_exposesNameAndDescription() {
        // when
        final CreateCourseRequest request = new CreateCourseRequest("name", "desc");

        // then
        assertThat(request.name()).isEqualTo("name");
        assertThat(request.description()).isEqualTo("desc");
    }

    @Test
    void createCourseRequest_equalInstances() {
        assertThat(new CreateCourseRequest("name", "desc"))
                .isEqualTo(new CreateCourseRequest("name", "desc"));
    }

    @Test
    void createCourseRequest_differentName_notEqual() {
        assertThat(new CreateCourseRequest("name1", "desc"))
                .isNotEqualTo(new CreateCourseRequest("name2", "desc"));
    }

    @Test
    void createdCourseResponse_exposesUuid() {
        // given
        final UUID uuid = UUID.randomUUID();

        // when
        final CreatedCourseResponse response = new CreatedCourseResponse(uuid);

        // then
        assertThat(response.uuid()).isEqualTo(uuid);
    }

    @Test
    void createdCourseResponse_equalInstances() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new CreatedCourseResponse(uuid))
                .isEqualTo(new CreatedCourseResponse(uuid));
    }
}
