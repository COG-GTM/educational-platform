package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalDTOTest {

    @Test
    void domainStatusConstructor_waitingForApproval_mapsToWaitingForApprovalDTO() {
        // given - CourseProposal.toDTO() and the admin listing read path build the DTO through this
        // convenience constructor, so the domain CourseProposalStatus must be mapped to its DTO counterpart
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatus.WAITING_FOR_APPROVAL);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void domainStatusConstructor_approved_mapsToApprovedDTO() {
        // given - an approved proposal must surface as the APPROVED DTO through the convenience constructor
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatus.APPROVED);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void domainStatusConstructor_declined_mapsToDeclinedDTO() {
        // given - a declined proposal must surface as the DECLINED DTO through the convenience constructor
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");

        // when
        final CourseProposalDTO dto = new CourseProposalDTO(uuid, CourseProposalStatus.DECLINED);

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }
}
