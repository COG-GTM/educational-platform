package com.educational.platform.jackson;

import com.educational.platform.courses.course.ApprovalStatus;
import com.educational.platform.courses.course.LectureType;
import com.educational.platform.courses.course.PublishStatus;
import com.educational.platform.administration.course.CourseProposalStatusDTO;
import com.educational.platform.course.enrollments.CompletionStatusDTO;
import com.educational.platform.users.RoleDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Jackson serialization/deserialization of all project enum types after the
 * Jackson Core upgrade (via Spring Boot BOM 4.0.1 -> 4.0.6). Covers domain-internal
 * enums (ApprovalStatus, PublishStatus, LectureType) that are not exercised elsewhere.
 */
class JacksonEnumSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    // --- ApprovalStatus (domain enum, courses module) ---

    @Test
    void approvalStatus_allValues_roundTrip() throws Exception {
        for (ApprovalStatus status : ApprovalStatus.values()) {
            var json = objectMapper.writeValueAsString(status);
            var deserialized = objectMapper.readValue(json, ApprovalStatus.class);
            assertThat(deserialized).isEqualTo(status);
        }
    }

    @Test
    void approvalStatus_serializedAsString() throws Exception {
        var json = objectMapper.writeValueAsString(ApprovalStatus.APPROVED);
        assertThat(json).isEqualTo("\"APPROVED\"");
    }

    @Test
    void approvalStatus_fromRawJson() throws Exception {
        var deserialized = objectMapper.readValue("\"WAITING_FOR_APPROVAL\"", ApprovalStatus.class);
        assertThat(deserialized).isEqualTo(ApprovalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void approvalStatus_invalidValue_throwsException() {
        assertThatThrownBy(() -> objectMapper.readValue("\"INVALID\"", ApprovalStatus.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void approvalStatus_nullValue_roundTrip() throws Exception {
        var json = objectMapper.writeValueAsString((ApprovalStatus) null);
        var deserialized = objectMapper.readValue(json, ApprovalStatus.class);
        assertThat(deserialized).isNull();
    }

    // --- PublishStatus (domain enum, courses module) ---

    @Test
    void publishStatus_allValues_roundTrip() throws Exception {
        for (PublishStatus status : PublishStatus.values()) {
            var json = objectMapper.writeValueAsString(status);
            var deserialized = objectMapper.readValue(json, PublishStatus.class);
            assertThat(deserialized).isEqualTo(status);
        }
    }

    @Test
    void publishStatus_serializedAsString() throws Exception {
        var json = objectMapper.writeValueAsString(PublishStatus.PUBLISHED);
        assertThat(json).isEqualTo("\"PUBLISHED\"");
    }

    @Test
    void publishStatus_fromRawJson() throws Exception {
        var deserialized = objectMapper.readValue("\"DRAFT\"", PublishStatus.class);
        assertThat(deserialized).isEqualTo(PublishStatus.DRAFT);
    }

    @Test
    void publishStatus_invalidValue_throwsException() {
        assertThatThrownBy(() -> objectMapper.readValue("\"UNKNOWN_STATUS\"", PublishStatus.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    // --- LectureType (domain enum, courses module) ---

    @Test
    void lectureType_allValues_roundTrip() throws Exception {
        for (LectureType type : LectureType.values()) {
            var json = objectMapper.writeValueAsString(type);
            var deserialized = objectMapper.readValue(json, LectureType.class);
            assertThat(deserialized).isEqualTo(type);
        }
    }

    @Test
    void lectureType_serializedAsString() throws Exception {
        var json = objectMapper.writeValueAsString(LectureType.TEXT);
        assertThat(json).isEqualTo("\"TEXT\"");
    }

    @Test
    void lectureType_fromRawJson() throws Exception {
        var deserialized = objectMapper.readValue("\"TEXT\"", LectureType.class);
        assertThat(deserialized).isEqualTo(LectureType.TEXT);
    }

    // --- Enum in collection contexts ---

    @Test
    void approvalStatusList_roundTrip() throws Exception {
        var list = Arrays.asList(ApprovalStatus.values());

        var json = objectMapper.writeValueAsString(list);
        var deserialized = objectMapper.readValue(json, new TypeReference<List<ApprovalStatus>>() {});

        assertThat(deserialized).containsExactlyElementsOf(list);
    }

    @Test
    void publishStatusList_roundTrip() throws Exception {
        var list = Arrays.asList(PublishStatus.values());

        var json = objectMapper.writeValueAsString(list);
        var deserialized = objectMapper.readValue(json, new TypeReference<List<PublishStatus>>() {});

        assertThat(deserialized).containsExactlyElementsOf(list);
    }

    @Test
    void enumMap_approvalStatusKeys_roundTrip() throws Exception {
        var map = Map.of(
                ApprovalStatus.APPROVED, "approved-desc",
                ApprovalStatus.DECLINED, "declined-desc"
        );

        var json = objectMapper.writeValueAsString(map);
        var deserialized = objectMapper.readValue(json,
                new TypeReference<Map<ApprovalStatus, String>>() {});

        assertThat(deserialized).hasSize(2);
        assertThat(deserialized.get(ApprovalStatus.APPROVED)).isEqualTo("approved-desc");
        assertThat(deserialized.get(ApprovalStatus.DECLINED)).isEqualTo("declined-desc");
    }

    // --- Enum in JSON tree API ---

    @Test
    void approvalStatus_viaJsonTree() throws Exception {
        var json = objectMapper.writeValueAsString(ApprovalStatus.NOT_SENT_FOR_APPROVAL);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.isTextual()).isTrue();
        assertThat(node.asText()).isEqualTo("NOT_SENT_FOR_APPROVAL");
    }

    @Test
    void publishStatus_viaValueToTree() throws Exception {
        JsonNode tree = objectMapper.valueToTree(PublishStatus.ARCHIVED);
        var deserialized = objectMapper.treeToValue(tree, PublishStatus.class);

        assertThat(deserialized).isEqualTo(PublishStatus.ARCHIVED);
    }

    // --- Enum case sensitivity ---

    @Test
    void approvalStatus_caseSensitive_lowercaseFails() {
        assertThatThrownBy(() -> objectMapper.readValue("\"approved\"", ApprovalStatus.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void publishStatus_caseSensitive_mixedCaseFails() {
        assertThatThrownBy(() -> objectMapper.readValue("\"Published\"", PublishStatus.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    // --- Enum ordinal consistency (verifying values() ordering after upgrade) ---

    @Test
    void approvalStatus_ordinalConsistency() {
        assertThat(ApprovalStatus.NOT_SENT_FOR_APPROVAL.ordinal()).isZero();
        assertThat(ApprovalStatus.WAITING_FOR_APPROVAL.ordinal()).isEqualTo(1);
        assertThat(ApprovalStatus.DECLINED.ordinal()).isEqualTo(2);
        assertThat(ApprovalStatus.APPROVED.ordinal()).isEqualTo(3);
    }

    @Test
    void publishStatus_ordinalConsistency() {
        assertThat(PublishStatus.DRAFT.ordinal()).isZero();
        assertThat(PublishStatus.PUBLISHED.ordinal()).isEqualTo(1);
        assertThat(PublishStatus.ARCHIVED.ordinal()).isEqualTo(2);
    }

    // --- Cross-module DTO enum consistency ---

    @Test
    void courseProposalStatusDTO_allValues_matchExpected() {
        assertThat(CourseProposalStatusDTO.values()).containsExactly(
                CourseProposalStatusDTO.WAITING_FOR_APPROVAL,
                CourseProposalStatusDTO.DECLINED,
                CourseProposalStatusDTO.APPROVED
        );
    }

    @Test
    void completionStatusDTO_allValues_matchExpected() {
        assertThat(CompletionStatusDTO.values()).containsExactly(
                CompletionStatusDTO.IN_PROGRESS,
                CompletionStatusDTO.COMPLETED
        );
    }

    @Test
    void roleDTO_allValues_matchExpected() {
        assertThat(RoleDTO.values()).containsExactly(
                RoleDTO.ROLE_STUDENT,
                RoleDTO.ROLE_TEACHER
        );
    }

    // --- Empty string and whitespace enum deserialization ---

    @Test
    void approvalStatus_emptyString_throwsException() {
        assertThatThrownBy(() -> objectMapper.readValue("\"\"", ApprovalStatus.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    void publishStatus_numericValue_throwsException() {
        assertThatThrownBy(() -> objectMapper.readValue("999", PublishStatus.class))
                .isInstanceOf(Exception.class);
    }
}
