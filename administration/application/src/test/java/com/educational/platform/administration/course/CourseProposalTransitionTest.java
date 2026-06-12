package com.educational.platform.administration.course;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CourseProposalTransitionTest {

    @Test
    void approve_declinedProposal_statusChangesToApproved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.decline();

        // when
        proposal.approve();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);
    }

    @Test
    void decline_approvedProposal_statusChangesToDeclined() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.approve();

        // when
        proposal.decline();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);
    }

    @Test
    void toDTO_afterApprove_statusIsApproved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_afterDecline_statusIsDeclined() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.uuid()).isEqualTo(uuid);
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void approve_thenApproveAgain_courseProposalAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.approve();

        // when
        final ThrowableAssert.ThrowingCallable approve = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class).isThrownBy(approve);
    }

    @Test
    void decline_thenDeclineAgain_courseProposalAlreadyDeclinedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.decline();

        // when
        final ThrowableAssert.ThrowingCallable decline = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class).isThrownBy(decline);
    }
}
