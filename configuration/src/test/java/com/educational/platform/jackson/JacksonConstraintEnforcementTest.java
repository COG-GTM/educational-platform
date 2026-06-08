package com.educational.platform.jackson;

import com.educational.platform.administration.course.CourseProposalDTO;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent;
import com.educational.platform.course.enrollments.CourseEnrollmentDTO;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.course.reviews.CourseReviewDTO;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import com.educational.platform.courses.CreateCourseRequest;
import com.educational.platform.courses.course.CourseRating;
import com.educational.platform.courses.course.NumberOfStudents;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
import com.educational.platform.users.UserDTO;
import com.educational.platform.users.RoleDTO;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import com.educational.platform.users.security.SignUpRequest;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that Jackson resource allocation constraints are enforced correctly across
 * domain-specific types, input sources, and ObjectMapper operations after the Jackson
 * Core upgrade (2.20.1 -> 2.21.2 via Spring Boot BOM 4.0.1 -> 4.0.6).
 *
 * Focuses on gaps not covered by JacksonResourceAllocationTest:
 * - ObjectMapper.copy() preserves constraints
 * - Constraints enforced via byte[] and Reader inputs
 * - Constraints enforced on real domain DTOs/events (not just generic JSON)
 * - Concurrent constraint enforcement
 * - Integration events from raw JSON (cross-module communication)
 * - Integration events unknown property rejection and field order independence
 */
class JacksonConstraintEnforcementTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // --- ObjectMapper.copy() preserves constraints ---

    @Test
    void copyPreservesReadConstraints() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(5)
                        .maxStringLength(50)
                        .maxNumberLength(10)
                        .build()
        );

        var copy = restrictedMapper.copy();
        var copyConstraints = copy.getFactory().streamReadConstraints();

        assertThat(copyConstraints.getMaxNestingDepth()).isEqualTo(5);
        assertThat(copyConstraints.getMaxStringLength()).isEqualTo(50);
        assertThat(copyConstraints.getMaxNumberLength()).isEqualTo(10);
    }

    @Test
    void copyPreservesWriteConstraints() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamWriteConstraints(
                StreamWriteConstraints.builder()
                        .maxNestingDepth(7)
                        .build()
        );

        var copy = restrictedMapper.copy();
        var copyConstraints = copy.getFactory().streamWriteConstraints();

        assertThat(copyConstraints.getMaxNestingDepth()).isEqualTo(7);
    }

    @Test
    void copiedMapper_enforcesConstraints() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(20)
                        .build()
        );

        var copy = restrictedMapper.copy();
        var longString = "{\"v\": \"" + "x".repeat(50) + "\"}";

        assertThatThrownBy(() -> copy.readTree(longString))
                .isInstanceOf(StreamConstraintsException.class);
    }

    // --- Constraints enforced via byte[] input ---

    @Test
    void byteArrayInput_constraintsEnforced_nestingDepth() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(5)
                        .build()
        );

        var json = buildNestedJson(10);
        var bytes = json.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> restrictedMapper.readValue(bytes, JsonNode.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void byteArrayInput_withinLimits_parsesSuccessfully() throws Exception {
        var json = "{\"name\":\"Test\",\"description\":\"Desc\"}";
        var bytes = json.getBytes(StandardCharsets.UTF_8);

        var result = objectMapper.readValue(bytes, CreateCourseRequest.class);

        assertThat(result.name()).isEqualTo("Test");
        assertThat(result.description()).isEqualTo("Desc");
    }

    // --- Constraints enforced via Reader input ---

    @Test
    void readerInput_constraintsEnforced_stringLength() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(20)
                        .build()
        );

        var longName = "x".repeat(50);
        var json = "{\"name\":\"" + longName + "\",\"description\":\"short\"}";

        assertThatThrownBy(() -> restrictedMapper.readValue(new StringReader(json), CreateCourseRequest.class))
                .hasRootCauseInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void readerInput_withinLimits_parsesSuccessfully() throws Exception {
        var json = "{\"name\":\"Test\",\"description\":\"Desc\"}";

        var result = objectMapper.readValue(new StringReader(json), CreateCourseRequest.class);

        assertThat(result.name()).isEqualTo("Test");
    }

    // --- Constraints on integration events ---

    @Test
    void constraintEnforced_onCourseApprovedEvent_longUuidString() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(10)
                        .build()
        );

        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        assertThatThrownBy(() -> restrictedMapper.readValue(json, CourseApprovedByAdminIntegrationEvent.class))
                .hasRootCauseInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void constraintEnforced_onStudentEnrolledEvent_longUsername() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(10)
                        .build()
        );

        var json = "{\"courseId\":\"" + UUID.randomUUID() + "\",\"username\":\"" + "a".repeat(50) + "\"}";

        assertThatThrownBy(() -> restrictedMapper.readValue(json, StudentEnrolledToCourseIntegrationEvent.class))
                .hasRootCauseInstanceOf(StreamConstraintsException.class);
    }

    // --- Integration events from raw JSON (cross-module communication) ---

    @Test
    void courseApprovedEvent_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, CourseApprovedByAdminIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void courseDeclinedEvent_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, CourseDeclinedByAdminIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void studentEnrolledEvent_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\",\"username\":\"student1\"}";

        var event = objectMapper.readValue(json, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.username()).isEqualTo("student1");
    }

    @Test
    void courseRatingRecalculatedEvent_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\",\"rating\":4.5}";

        var event = objectMapper.readValue(json, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.rating()).isEqualTo(4.5);
    }

    @Test
    void sendCourseToApproveEvent_fromRawJson() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, SendCourseToApproveIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
    }

    @Test
    void userCreatedEvent_fromRawJson() throws Exception {
        var json = "{\"username\":\"newuser\",\"email\":\"new@example.com\"}";

        var event = objectMapper.readValue(json, UserCreatedIntegrationEvent.class);

        assertThat(event.username()).isEqualTo("newuser");
        assertThat(event.email()).isEqualTo("new@example.com");
    }

    // --- Integration events field order independence ---

    @Test
    void studentEnrolledEvent_fieldOrderIndependence() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"username\":\"student1\",\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.username()).isEqualTo("student1");
    }

    @Test
    void courseRatingRecalculatedEvent_fieldOrderIndependence() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"rating\":3.75,\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.rating()).isEqualTo(3.75);
    }

    @Test
    void userCreatedEvent_fieldOrderIndependence() throws Exception {
        var json = "{\"email\":\"user@test.com\",\"username\":\"testuser\"}";

        var event = objectMapper.readValue(json, UserCreatedIntegrationEvent.class);

        assertThat(event.username()).isEqualTo("testuser");
        assertThat(event.email()).isEqualTo("user@test.com");
    }

    // --- Integration events unknown property rejection ---

    @Test
    void courseApprovedEvent_unknownProperty_rejected() {
        var json = "{\"courseId\":\"" + UUID.randomUUID() + "\",\"extra\":true}";

        assertThatThrownBy(() -> objectMapper.readValue(json, CourseApprovedByAdminIntegrationEvent.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class)
                .hasMessageContaining("extra");
    }

    @Test
    void studentEnrolledEvent_unknownProperty_rejected() {
        var json = "{\"courseId\":\"" + UUID.randomUUID() + "\",\"username\":\"u\",\"unknown\":1}";

        assertThatThrownBy(() -> objectMapper.readValue(json, StudentEnrolledToCourseIntegrationEvent.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void userCreatedEvent_unknownProperty_rejected() {
        var json = "{\"username\":\"u\",\"email\":\"e\",\"role\":\"admin\"}";

        assertThatThrownBy(() -> objectMapper.readValue(json, UserCreatedIntegrationEvent.class))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class)
                .hasMessageContaining("role");
    }

    // --- Integration events with edge case values ---

    @Test
    void courseRatingRecalculatedEvent_negativeRating_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new CourseRatingRecalculatedIntegrationEvent(uuid, -1.0);

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(deserialized.rating()).isEqualTo(-1.0);
    }

    @Test
    void studentEnrolledEvent_emptyUsername_roundTrip() throws Exception {
        var uuid = UUID.randomUUID();
        var original = new StudentEnrolledToCourseIntegrationEvent(uuid, "");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(deserialized.username()).isEmpty();
    }

    @Test
    void userCreatedEvent_emptyFields_roundTrip() throws Exception {
        var original = new UserCreatedIntegrationEvent("", "");

        var json = objectMapper.writeValueAsString(original);
        var deserialized = objectMapper.readValue(json, UserCreatedIntegrationEvent.class);

        assertThat(deserialized.username()).isEmpty();
        assertThat(deserialized.email()).isEmpty();
    }

    // --- Concurrent constraint enforcement ---

    @Test
    void concurrentConstraintEnforcement_isThreadSafe() throws Exception {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNestingDepth(5)
                        .build()
        );

        var threads = 10;
        var latch = new CountDownLatch(1);
        var constraintViolations = new AtomicInteger(0);
        var unexpectedErrors = new AtomicInteger(0);
        var executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    var deepJson = buildNestedJson(10);
                    restrictedMapper.readTree(deepJson);
                    unexpectedErrors.incrementAndGet();
                } catch (StreamConstraintsException e) {
                    constraintViolations.incrementAndGet();
                } catch (Exception e) {
                    unexpectedErrors.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(constraintViolations.get()).isEqualTo(threads);
        assertThat(unexpectedErrors.get()).isZero();
    }

    @Test
    void concurrentDomainTypeParsing_withConstraints_isThreadSafe() throws Exception {
        var threads = 10;
        var latch = new CountDownLatch(1);
        var errors = new AtomicInteger(0);
        var executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    var uuid = UUID.randomUUID();
                    var original = new CourseRatingRecalculatedIntegrationEvent(uuid, idx * 0.5);
                    var json = objectMapper.writeValueAsString(original);
                    var result = objectMapper.readValue(json, CourseRatingRecalculatedIntegrationEvent.class);
                    assertThat(result.courseId()).isEqualTo(uuid);
                    assertThat(result.rating()).isEqualTo(idx * 0.5);
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(errors.get()).isZero();
    }

    // --- Constraints on domain DTO deserialization via different input sources ---

    @Test
    void courseReviewDTO_viaByteArray_parsesSuccessfully() throws Exception {
        var uuid = UUID.randomUUID();
        var courseUuid = UUID.randomUUID();
        var json = "{\"uuid\":\"" + uuid + "\",\"course\":\"" + courseUuid
                + "\",\"username\":\"user1\",\"comment\":\"Great\",\"rating\":4.5}";

        var result = objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), CourseReviewDTO.class);

        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.course()).isEqualTo(courseUuid);
        assertThat(result.rating()).isEqualTo(4.5);
    }

    @Test
    void courseEnrollmentDTO_viaReader_parsesSuccessfully() throws Exception {
        var uuid = UUID.randomUUID();
        var courseUuid = UUID.randomUUID();
        var json = "{\"uuid\":\"" + uuid + "\",\"course\":\"" + courseUuid
                + "\",\"student\":\"s1\",\"completionStatus\":\"IN_PROGRESS\"}";

        var result = objectMapper.readValue(new StringReader(json), CourseEnrollmentDTO.class);

        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.completionStatus()).isEqualTo(CompletionStatusDTO.IN_PROGRESS);
    }

    @Test
    void courseProposalDTO_viaInputStream_parsesSuccessfully() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"uuid\":\"" + uuid + "\",\"status\":\"APPROVED\"}";
        var inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        var result = objectMapper.readValue(inputStream, CourseProposalDTO.class);

        assertThat(result.uuid()).isEqualTo(uuid);
        assertThat(result.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void userDTO_viaByteArray_parsesSuccessfully() throws Exception {
        var json = "{\"username\":\"admin\",\"email\":\"a@b.com\",\"role\":\"ROLE_TEACHER\"}";

        var result = objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), UserDTO.class);

        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.role()).isEqualTo(RoleDTO.ROLE_TEACHER);
    }

    @Test
    void signUpRequest_viaReader_parsesSuccessfully() throws Exception {
        var json = "{\"role\":\"ROLE_STUDENT\",\"username\":\"john\",\"email\":\"j@e.com\",\"password\":\"pass\"}";

        var result = objectMapper.readValue(new StringReader(json), SignUpRequest.class);

        assertThat(result.username()).isEqualTo("john");
        assertThat(result.role()).isEqualTo(RoleDTO.ROLE_STUDENT);
    }

    // --- Constraint enforcement via byte[] on deeply nested JSON ---

    @Test
    void byteArrayInput_deeplyNestedJson_constraintEnforced() {
        var json = buildNestedJson(1500);
        var bytes = json.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> objectMapper.readValue(bytes, JsonNode.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void readerInput_deeplyNestedJson_constraintEnforced() {
        var json = buildNestedJson(1500);

        assertThatThrownBy(() -> objectMapper.readValue(new StringReader(json), JsonNode.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    // --- Integration events missing fields default to null ---

    @Test
    void studentEnrolledEvent_missingUsername_defaultsToNull() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, StudentEnrolledToCourseIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.username()).isNull();
    }

    @Test
    void userCreatedEvent_missingEmail_defaultsToNull() throws Exception {
        var json = "{\"username\":\"user1\"}";

        var event = objectMapper.readValue(json, UserCreatedIntegrationEvent.class);

        assertThat(event.username()).isEqualTo("user1");
        assertThat(event.email()).isNull();
    }

    @Test
    void courseRatingRecalculatedEvent_missingRating_defaultsToZero() throws Exception {
        var uuid = UUID.randomUUID();
        var json = "{\"courseId\":\"" + uuid + "\"}";

        var event = objectMapper.readValue(json, CourseRatingRecalculatedIntegrationEvent.class);

        assertThat(event.courseId()).isEqualTo(uuid);
        assertThat(event.rating()).isEqualTo(0.0);
    }

    // --- NumberOfStudents and CourseRating constraint enforcement via byte[] ---

    @Test
    void courseRating_constraintEnforced_viaByteArray() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNumberLength(3)
                        .build()
        );

        var json = "{\"rating\": 12345.67890}";
        var bytes = json.getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> restrictedMapper.readValue(bytes, CourseRating.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    @Test
    void numberOfStudents_constraintEnforced_viaReader() {
        var restrictedMapper = new ObjectMapper();
        restrictedMapper.findAndRegisterModules();
        restrictedMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxNumberLength(3)
                        .build()
        );

        var json = "{\"number\": 1234567}";

        assertThatThrownBy(() -> restrictedMapper.readValue(new StringReader(json), NumberOfStudents.class))
                .isInstanceOf(StreamConstraintsException.class);
    }

    private String buildNestedJson(int depth) {
        var sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{\"a\":");
        }
        sb.append("1");
        for (int i = 0; i < depth; i++) {
            sb.append("}");
        }
        return sb.toString();
    }
}
