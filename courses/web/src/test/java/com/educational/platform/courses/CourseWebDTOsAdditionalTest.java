package com.educational.platform.courses;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseWebDTOsAdditionalTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID UUID_VALUE_2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

    @Test
    void createCourseRequest_differentName_notEqual() {
        assertThat(new CreateCourseRequest("name1", "desc"))
                .isNotEqualTo(new CreateCourseRequest("name2", "desc"));
    }

    @Test
    void createCourseRequest_differentDescription_notEqual() {
        assertThat(new CreateCourseRequest("name", "desc1"))
                .isNotEqualTo(new CreateCourseRequest("name", "desc2"));
    }

    @Test
    void createdCourseResponse_differentUuid_notEqual() {
        assertThat(new CreatedCourseResponse(UUID_VALUE))
                .isNotEqualTo(new CreatedCourseResponse(UUID_VALUE_2));
    }

    @Test
    void createCourseRequest_hashCodeConsistentWithEquals() {
        final CreateCourseRequest first = new CreateCourseRequest("name", "desc");
        final CreateCourseRequest second = new CreateCourseRequest("name", "desc");
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
