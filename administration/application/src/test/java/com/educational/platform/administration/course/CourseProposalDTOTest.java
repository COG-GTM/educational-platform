package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
