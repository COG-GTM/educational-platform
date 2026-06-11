package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseProposalDTOTest {

    @Test
    void primaryConstructor_preservesValues() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void convenienceConstructor_convertsStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatus.WAITING_FOR_APPROVAL);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void convenienceConstructor_equivalentToPrimaryConstructor() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO fromStatus = new CourseProposalDTO(uuid, CourseProposalStatus.DECLINED);
        final CourseProposalDTO fromStatusDTO = new CourseProposalDTO(uuid, CourseProposalStatusDTO.DECLINED);

        // then
        assertThat(fromStatus).isEqualTo(fromStatusDTO);
    }

    @ParameterizedTest
    @EnumSource(CourseProposalStatus.class)
    void convenienceConstructor_allStatuses_nonNullDTO(CourseProposalStatus status) {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, status);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isNotNull();
        assertThat(dto.status()).isEqualTo(status.toDTO());
    }

    @Test
    void recordEquality_samValues_equal() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto1 = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);
        final CourseProposalDTO dto2 = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);

        // then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void recordEquality_differentStatus_notEqual() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto1 = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);
        final CourseProposalDTO dto2 = new CourseProposalDTO(uuid, CourseProposalStatusDTO.DECLINED);

        // then
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void convenienceConstructor_nullStatus_throwsNullPointerException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when / then
        assertThatThrownBy(() -> new CourseProposalDTO(uuid, (CourseProposalStatus) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void primaryConstructor_nullValues_preservesNulls() {
        // when
        final CourseProposalDTO dto = new CourseProposalDTO(null, (CourseProposalStatusDTO) null);

        // then
        assertThat(dto.uuid()).isNull();
        assertThat(dto.status()).isNull();
    }

    @Test
    void recordEquality_differentUuid_notEqual() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseProposalDTO dto1 = new CourseProposalDTO(uuid1, CourseProposalStatusDTO.APPROVED);
        final CourseProposalDTO dto2 = new CourseProposalDTO(uuid2, CourseProposalStatusDTO.APPROVED);

        // then
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void recordEquality_bothNullUuids_sameStatus_equal() {
        // when
        final CourseProposalDTO dto1 = new CourseProposalDTO(null, CourseProposalStatusDTO.APPROVED);
        final CourseProposalDTO dto2 = new CourseProposalDTO(null, CourseProposalStatusDTO.APPROVED);

        // then
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void toString_containsUuidAndStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatusDTO.APPROVED);

        // then
        assertThat(dto.toString())
                .contains(uuid.toString())
                .contains("APPROVED");
    }
}
