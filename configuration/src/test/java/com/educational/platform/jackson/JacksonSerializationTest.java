package com.educational.platform.jackson;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.courses.CreateCourseRequest;
import com.educational.platform.courses.CreatedCourseResponse;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.security.SignInRequest;
import com.educational.platform.users.security.SignInResponse;
import com.educational.platform.users.security.SignUpRequest;
import com.educational.platform.web.handler.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Jackson serialization/deserialization of the project's record-based
 * DTOs remains compatible after upgrading Jackson Core (via Spring Boot BOM 4.0.1 -> 4.0.6).
 */
class JacksonSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void createCourseRequest_roundTrip() throws Exception {
        var original = new CreateCourseRequest("Java Basics", "Introduction to Java");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseRequest.class);

        assertThat(deserialized.name()).isEqualTo("Java Basics");
        assertThat(deserialized.description()).isEqualTo("Introduction to Java");
    }

    @Test
    void createdCourseResponse_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CreatedCourseResponse(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreatedCourseResponse.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void signUpRequest_roundTrip() throws Exception {
        var original = new SignUpRequest(RoleDTO.ROLE_STUDENT, "john", "john@example.com", "P@ssword1");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SignUpRequest.class);

        assertThat(deserialized.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
        assertThat(deserialized.username()).isEqualTo("john");
        assertThat(deserialized.email()).isEqualTo("john@example.com");
        assertThat(deserialized.password()).isEqualTo("P@ssword1");
    }

    @Test
    void signInRequest_roundTrip() throws Exception {
        var original = new SignInRequest("admin", "secret");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SignInRequest.class);

        assertThat(deserialized.username()).isEqualTo("admin");
        assertThat(deserialized.password()).isEqualTo("secret");
    }

    @Test
    void signInResponse_roundTrip() throws Exception {
        var original = new SignInResponse("jwt-token-123");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, SignInResponse.class);

        assertThat(deserialized.token()).isEqualTo("jwt-token-123");
    }

    @Test
    void courseReviewDTO_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var courseUuid = UUID.randomUUID();
        var original = new CourseReviewDTO(uuid, courseUuid, "student1", "Great course!", 4.5);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseReviewDTO.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.course()).isEqualTo(courseUuid);
        assertThat(deserialized.username()).isEqualTo("student1");
        assertThat(deserialized.comment()).isEqualTo("Great course!");
        assertThat(deserialized.rating()).isEqualTo(4.5);
    }

    @Test
    void courseProposalDTO_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseProposalDTO.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void courseEnrollmentDTO_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var courseUuid = UUID.randomUUID();
        var original = new CourseEnrollmentDTO(uuid, courseUuid, "student1", CompletionStatusDTO.IN_PROGRESS);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseEnrollmentDTO.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.course()).isEqualTo(courseUuid);
        assertThat(deserialized.student()).isEqualTo("student1");
        assertThat(deserialized.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void errorResponse_singleError_roundTrip() throws Exception {
        var original = new ErrorResponse("Something went wrong");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ErrorResponse.class);

        assertThat(deserialized.errors()).containsExactly("Something went wrong");
    }

    @Test
    void errorResponse_multipleErrors_roundTrip() throws Exception {
        var original = new ErrorResponse(List.of("Error 1", "Error 2", "Error 3"));

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ErrorResponse.class);

        assertThat(deserialized.errors()).containsExactly("Error 1", "Error 2", "Error 3");
    }

    @Test
    void nullFields_serializedCorrectly() throws Exception {
        var original = new CourseReviewDTO(null, null, null, null, 0.0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseReviewDTO.class);

        assertThat(deserialized.uuid()).isNull();
        assertThat(deserialized.course()).isNull();
        assertThat(deserialized.username()).isNull();
        assertThat(deserialized.comment()).isNull();
        assertThat(deserialized.rating()).isEqualTo(0.0);
    }

    @Test
    void emptyStringFields_serializedCorrectly() throws Exception {
        var original = new CreateCourseRequest("", "");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseRequest.class);

        assertThat(deserialized.name()).isEmpty();
        assertThat(deserialized.description()).isEmpty();
    }

    @Test
    void enumValues_allRoles_roundTrip() throws Exception {
        for (RoleDTO role : RoleDTO.values()) {
            var original = new SignUpRequest(role, "user", "user@test.com", "pass");
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, SignUpRequest.class);
            assertThat(deserialized.role()).isEqualTo(role);
        }
    }

    @Test
    void enumValues_allCompletionStatuses_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var courseUuid = UUID.randomUUID();
        for (CompletionStatusDTO status : CompletionStatusDTO.values()) {
            var original = new CourseEnrollmentDTO(uuid, courseUuid, "student", status);
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, CourseEnrollmentDTO.class);
            assertThat(deserialized.completionStatus()).isEqualTo(status);
        }
    }

    @Test
    void enumValues_allProposalStatuses_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        for (CourseProposalStatusDTO status : CourseProposalStatusDTO.values()) {
            var original = new CourseProposalDTO(uuid, status);
            var json = objectMapper.writeValueAsString(original);
            var deserialized = objectMapper.readValue(json, CourseProposalDTO.class);
            assertThat(deserialized.status()).isEqualTo(status);
        }
    }
}
