package com.educational.platform.courses.bridge;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationEventJsonTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void payloads_matchPythonSchemas() {
        assertThat(IntegrationEventJson.courseId(COURSE_ID))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\"}");
        assertThat(IntegrationEventJson.courseIdAndUsername(COURSE_ID, "stu\"dent"))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"username\":\"stu\\\"dent\"}");
        assertThat(IntegrationEventJson.usernameAndEmail("user", null))
                .isEqualTo("{\"username\":\"user\",\"email\":null}");
        assertThat(IntegrationEventJson.courseIdAndRating(COURSE_ID, 4.5))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\",\"rating\":4.5}");
    }

    @Test
    void readCourseId_pythonPayload_courseId() {
        assertThat(IntegrationEventJson.readCourseId("{\"courseId\": \"123e4567-e89b-12d3-a456-426655440001\"}"))
                .contains(COURSE_ID);
        assertThat(IntegrationEventJson.readCourseId("{\"username\": \"x\"}")).isEmpty();
    }
}
