package com.educational.platform.administration.course;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link CourseProposal#toDTO()} reflects the domain state after transitions.
 */
public class CourseProposalStateDTOTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void toDTO_afterCreation_statusIsWaitingForApproval() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_afterApprove_statusIsApproved() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_afterDecline_statusIsDeclined() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void toDTO_afterDeclineThenApprove_statusIsApproved() {
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
    void toDTO_afterApproveThenDecline_statusIsDeclined() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
        proposal.approve();
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void toDTO_preservesUuid() {
        // given
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.uuid()).isEqualTo(UUID_VALUE);
    }
}
