package com.educational.platform.courses.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void string_escapesJsonControlCharacters() {
        assertThat(IntegrationEventJson.courseIdAndUsername(COURSE_ID, "a\\b\n\r\t\u0001"))
                .isEqualTo("{\"courseId\":\"123e4567-e89b-12d3-a456-426655440001\","
                        + "\"username\":\"a\\\\b\\n\\r\\t\\u0001\"}");
        assertThat(IntegrationEventJson.usernameAndEmail(null, ""))
                .isEqualTo("{\"username\":null,\"email\":\"\"}");
        assertThat(IntegrationEventJson.usernameAndEmail("ünïcødé", "e@x.io"))
                .isEqualTo("{\"username\":\"ünïcødé\",\"email\":\"e@x.io\"}");
    }

    @Test
    void courseIdAndRating_javaDoubleFormatting() {
        assertThat(IntegrationEventJson.courseIdAndRating(COURSE_ID, 4))
                .endsWith("\"rating\":4.0}");
        assertThat(IntegrationEventJson.courseIdAndRating(COURSE_ID, 0))
                .endsWith("\"rating\":0.0}");
        assertThat(IntegrationEventJson.courseIdAndRating(COURSE_ID, 3.3333333333333335))
                .endsWith("\"rating\":3.3333333333333335}");
    }

    @Test
    void readCourseId_roundTripsOwnSerialisation() {
        assertThat(IntegrationEventJson.readCourseId(IntegrationEventJson.courseId(COURSE_ID))).contains(COURSE_ID);
        assertThat(IntegrationEventJson.readCourseId(IntegrationEventJson.courseIdAndUsername(COURSE_ID, "u")))
                .contains(COURSE_ID);
        assertThat(IntegrationEventJson.readCourseId(IntegrationEventJson.courseIdAndRating(COURSE_ID, 1.5)))
                .contains(COURSE_ID);
    }

    @Test
    void readCourseId_whitespaceAndKeyOrderIndependent() {
        assertThat(IntegrationEventJson.readCourseId(
                "{ \"username\" : \"u\" ,\n  \"courseId\"\t:\n\"123e4567-e89b-12d3-a456-426655440001\" }"))
                .contains(COURSE_ID);
        assertThat(IntegrationEventJson.readCourseId("{\"courseId\":\"123E4567-E89B-12D3-A456-426655440001\"}"))
                .contains(COURSE_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "{}",
            "[]",
            "null",
            "{\"courseId\":null}",
            "{\"courseId\":123}",
            "{\"courseId\":\"\"}",
            "{\"courseId\":\"123e4567-e89b-12d3-a456\"}",
            "{\"courseId\":\"123e4567-e89b-12d3-a456-42665544000g\"}",
            "{\"course_id\":\"123e4567-e89b-12d3-a456-426655440001\"}",
            "{\"CourseId\":\"123e4567-e89b-12d3-a456-426655440001\"}"
    })
    void readCourseId_noValidCourseId_empty(String json) {
        assertThat(IntegrationEventJson.readCourseId(json)).isEmpty();
    }

    @Test
    void readCourseId_nullJson_nullPointerException() {
        assertThatThrownBy(() -> IntegrationEventJson.readCourseId(null)).isInstanceOf(NullPointerException.class);
    }
}
