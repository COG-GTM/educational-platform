package com.educational.platform.jackson;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CourseEnrollmentRequest;
import com.educational.platform.course.reviews.CourseReviewCreatedResponse;
import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.ReviewCourseRequest;
import com.educational.platform.course.reviews.UpdateCourseReviewRequest;
import com.educational.platform.courses.CreateCourseRequest;
import com.educational.platform.courses.CreatedCourseResponse;
import com.educational.platform.courses.course.CourseLightDTO;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.UserDTO;
import com.educational.platform.users.security.SignInRequest;
import com.educational.platform.users.security.SignInResponse;
import com.educational.platform.users.security.SignUpRequest;
import com.educational.platform.web.handler.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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

    @Test
    void courseEnrollmentRequest_roundTrip() throws Exception {
        var original = new CourseEnrollmentRequest("student1");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseEnrollmentRequest.class);

        assertThat(deserialized.student()).isEqualTo("student1");
    }

    @Test
    void reviewCourseRequest_roundTrip() throws Exception {
        var original = new ReviewCourseRequest(4.5, "Excellent content");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseRequest.class);

        assertThat(deserialized.rating()).isEqualTo(4.5);
        assertThat(deserialized.comment()).isEqualTo("Excellent content");
    }

    @Test
    void updateCourseReviewRequest_roundTrip() throws Exception {
        var original = new UpdateCourseReviewRequest(3.0, "Updated review");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UpdateCourseReviewRequest.class);

        assertThat(deserialized.rating()).isEqualTo(3.0);
        assertThat(deserialized.comment()).isEqualTo("Updated review");
    }

    @Test
    void courseReviewCreatedResponse_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseReviewCreatedResponse(uuid);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseReviewCreatedResponse.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
    }

    @Test
    void userDTO_roundTrip() throws Exception {
        var original = new UserDTO("admin", "admin@example.com", RoleDTO.ROLE_TEACHER);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UserDTO.class);

        assertThat(deserialized.username()).isEqualTo("admin");
        assertThat(deserialized.email()).isEqualTo("admin@example.com");
        assertThat(deserialized.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void courseLightDTO_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseLightDTO(uuid, "Advanced Java", "Deep dive", 150);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseLightDTO.class);

        assertThat(deserialized.uuid()).isEqualTo(uuid);
        assertThat(deserialized.name()).isEqualTo("Advanced Java");
        assertThat(deserialized.description()).isEqualTo("Deep dive");
        assertThat(deserialized.numberOfStudents()).isEqualTo(150);
    }

    @Test
    void specialCharacters_roundTrip() throws Exception {
        var original = new CreateCourseRequest(
                "Caf\u00e9 & R\u00e9sum\u00e9 \u2014 \u201cAdvanced\u201d Topics",
                "Description with unicode: \u00e9\u00f1\u00fc and newline\nand tab\t"
        );

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CreateCourseRequest.class);

        assertThat(deserialized.name()).isEqualTo("Caf\u00e9 & R\u00e9sum\u00e9 \u2014 \u201cAdvanced\u201d Topics");
        assertThat(deserialized.description()).contains("\u00e9", "\u00f1", "\u00fc");
    }

    @Test
    void errorResponse_nullError_createsEmptyList() throws Exception {
        var original = new ErrorResponse((String) null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ErrorResponse.class);

        assertThat(deserialized.errors()).isEmpty();
    }

    @Test
    void errorResponse_emptyList_roundTrip() throws Exception {
        var original = new ErrorResponse(Collections.emptyList());

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ErrorResponse.class);

        assertThat(deserialized.errors()).isEmpty();
    }

    @Test
    void reviewCourseRequest_nullComment_roundTrip() throws Exception {
        var original = new ReviewCourseRequest(5.0, null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseRequest.class);

        assertThat(deserialized.rating()).isEqualTo(5.0);
        assertThat(deserialized.comment()).isNull();
    }

    @Test
    void reviewCourseRequest_zeroRating_roundTrip() throws Exception {
        var original = new ReviewCourseRequest(0.0, "No comment");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, ReviewCourseRequest.class);

        assertThat(deserialized.rating()).isEqualTo(0.0);
    }

    @Test
    void courseLightDTO_zeroStudents_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseLightDTO(uuid, "Empty Course", "No students yet", 0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseLightDTO.class);

        assertThat(deserialized.numberOfStudents()).isZero();
    }

    @Test
    void deserializationFromRawJson_createCourseRequest() throws Exception {
        var json = "{\"name\":\"Spring Boot\",\"description\":\"Learn Spring Boot\"}";

        var deserialized = objectMapper.readValue(json, CreateCourseRequest.class);

        assertThat(deserialized.name()).isEqualTo("Spring Boot");
        assertThat(deserialized.description()).isEqualTo("Learn Spring Boot");
    }

    @Test
    void jsonFieldNames_matchRecordComponents() throws Exception {
        var original = new CreateCourseRequest("Test", "Desc");

        var json = objectMapper.writeValueAsString(original);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.has("name")).isTrue();
        assertThat(node.has("description")).isTrue();
        assertThat(node.size()).isEqualTo(2);
    }

    @Test
    void courseEnrollmentRequest_nullStudent_roundTrip() throws Exception {
        var original = new CourseEnrollmentRequest(null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseEnrollmentRequest.class);

        assertThat(deserialized.student()).isNull();
    }

    @Test
    void userDTO_nullFields_roundTrip() throws Exception {
        var original = new UserDTO(null, null, null);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UserDTO.class);

        assertThat(deserialized.username()).isNull();
        assertThat(deserialized.email()).isNull();
        assertThat(deserialized.role()).isNull();
    }
}
