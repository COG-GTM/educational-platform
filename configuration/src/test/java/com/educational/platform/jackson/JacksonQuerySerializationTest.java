package com.educational.platform.jackson;

import com.educational.platform.administration.course.query.ListCourseProposalsQuery;
import com.educational.platform.course.enrollments.query.CourseEnrollmentByUUIDQuery;
import com.educational.platform.course.enrollments.query.ListCourseEnrollmentsQuery;
import com.educational.platform.course.reviews.query.ListCourseReviewsByCourseUUIDQuery;
import com.educational.platform.courses.course.query.CourseByUUIDQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Jackson serialization/deserialization of query record types after the
 * Jackson Core upgrade (via Spring Boot BOM 4.0.1 -> 4.0.6). Query records are used
 * for the read side of CQRS and must remain serializable for caching/logging.
 */
class JacksonQuerySerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // --- CourseByUUIDQuery ---

    @Test
    void courseByUUIDQuery_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseByUUIDQuery(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseByUUIDQuery.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void courseByUUIDQuery_nullUuid_roundTrip() throws Exception {
        var original = new CourseByUUIDQuery(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseByUUIDQuery.class);

        assertThat(deserialized.uuid()).isNull();
    }

    @Test
    void courseByUUIDQuery_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"uuid\":\"" + uuid + "\"}";

        var deserialized = objectMapper.readValue(json, CourseByUUIDQuery.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void courseByUUIDQuery_unknownProperties_rejected() {
        var json = "{\"uuid\":\"" + UUID.randomUUID() + "\",\"extra\":true}";

        assertThatThrownBy(() -> objectMapper.readValue(json, CourseByUUIDQuery.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining("extra");
    }

    // --- CourseEnrollmentByUUIDQuery ---

    @Test
    void courseEnrollmentByUUIDQuery_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseEnrollmentByUUIDQuery(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseEnrollmentByUUIDQuery.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void courseEnrollmentByUUIDQuery_nullUuid_roundTrip() throws Exception {
        var original = new CourseEnrollmentByUUIDQuery(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseEnrollmentByUUIDQuery.class);

        assertThat(deserialized.uuid()).isNull();
    }

    // --- ListCourseReviewsByCourseUUIDQuery ---

    @Test
    void listCourseReviewsByCourseUUIDQuery_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new ListCourseReviewsByCourseUUIDQuery(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ListCourseReviewsByCourseUUIDQuery.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void listCourseReviewsByCourseUUIDQuery_nullUuid_roundTrip() throws Exception {
        var original = new ListCourseReviewsByCourseUUIDQuery(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ListCourseReviewsByCourseUUIDQuery.class);

        assertThat(deserialized.uuid()).isNull();
    }

    // --- Parameterless query records ---

    @Test
    void listCourseProposalsQuery_roundTrip() throws Exception {
        var original = new ListCourseProposalsQuery();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ListCourseProposalsQuery.class);

        assertThat(deserialized).isNotNull();
        assertThat(json).isEqualTo("{}");
    }

    @Test
    void listCourseEnrollmentsQuery_roundTrip() throws Exception {
        var original = new ListCourseEnrollmentsQuery();

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ListCourseEnrollmentsQuery.class);

        assertThat(deserialized).isNotNull();
        assertThat(json).isEqualTo("{}");
    }

    @Test
    void listCourseProposalsQuery_emptyJsonObject_deserializes() throws Exception {
        var deserialized = objectMapper.readValue("{}", ListCourseProposalsQuery.class);

        assertThat(deserialized).isNotNull();
    }

    @Test
    void listCourseEnrollmentsQuery_unknownProperties_rejected() {
        var json = "{\"unexpected\":\"value\"}";

        assertThatThrownBy(() -> objectMapper.readValue(json, ListCourseEnrollmentsQuery.class))
                .isInstanceOf(UnrecognizedPropertyException.class)
                .hasMessageContaining("unexpected");
    }

    // --- Field order independence ---

    @Test
    void courseByUUIDQuery_fromRawJson_fieldOrderIndependence() throws Exception {
        var uuid = UUID.randomUUID();
        var json1 = "{\"uuid\":\"" + uuid + "\"}";
        var json2 = "{\"uuid\":\"" + uuid + "\"}";

        var d1 = objectMapper.readValue(json1, CourseByUUIDQuery.class);
        var d2 = objectMapper.readValue(json2, CourseByUUIDQuery.class);

        assertThat(d1.uuid()).isEqualTo(d2.uuid());
    }
}
