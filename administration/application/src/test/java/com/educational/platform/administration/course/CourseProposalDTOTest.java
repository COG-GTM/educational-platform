package com.educational.platform.administration.course;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseProposalDTOTest {

    @Test
    void constructor_withUuidAndStatusDTO_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalStatusDTO status = CourseProposalStatusDTO.WAITING_FOR_APPROVAL;

        // when
        final CourseProposalDTO sut = new CourseProposalDTO(uuid, status);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void constructor_withUuidAndDomainStatus_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalStatus status = CourseProposalStatus.APPROVED;

        // when
        final CourseProposalDTO sut = new CourseProposalDTO(uuid, status);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void constructor_declinedStatus_dtoCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposalStatus status = CourseProposalStatus.DECLINED;

        // when
        final CourseProposalDTO sut = new CourseProposalDTO(uuid, status);

        // then
        assertThat(sut)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.DECLINED);
    }
}
