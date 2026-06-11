package com.educational.platform.administration.course;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link CourseProposal#toDTO()} after various state transitions,
 * verifying that the DTO reflects the correct status.
 */
public class CourseProposalToDTOStatesTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void toDTO_afterCreation_statusWaitingForApproval() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_afterApproval_statusApproved() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_afterDecline_statusDeclined() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void toDTO_afterDeclineThenApprove_statusApproved() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
        proposal.decline();
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_preservesUuid() {
        // given
        final UUID uuid = UUID.randomUUID();
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
    }
}
