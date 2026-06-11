package com.educational.platform.administration.course;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the domain-level guard clauses on {@link CourseProposal#approve()} and {@link CourseProposal#decline()}.
 */
public class CourseProposalGuardClausesTest {

    private static final UUID UUID_VALUE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Test
    void approve_waitingForApproval_succeeds() {
        // given
        final CourseProposal proposal = newProposal();

        // when
        proposal.approve();

        // then
        assertThat(proposal.toDTO().status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void approve_alreadyApproved_throwsCourseProposalAlreadyApprovedException() {
        // given
        final CourseProposal proposal = newProposal();
        proposal.approve();

        // when
        final ThrowableAssert.ThrowingCallable action = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class)
                .isThrownBy(action)
                .satisfies(ex -> assertThat(ex.getMessage()).contains(UUID_VALUE.toString()));
    }

    @Test
    void decline_waitingForApproval_succeeds() {
        // given
        final CourseProposal proposal = newProposal();

        // when
        proposal.decline();

        // then
        assertThat(proposal.toDTO().status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void decline_alreadyDeclined_throwsCourseProposalAlreadyDeclinedException() {
        // given
        final CourseProposal proposal = newProposal();
        proposal.decline();

        // when
        final ThrowableAssert.ThrowingCallable action = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class)
                .isThrownBy(action)
                .satisfies(ex -> assertThat(ex.getMessage()).contains(UUID_VALUE.toString()));
    }

    @Test
    void approve_afterDecline_succeeds() {
        // given
        final CourseProposal proposal = newProposal();
        proposal.decline();

        // when
        proposal.approve();

        // then
        assertThat(proposal.toDTO().status()).isEqualTo(CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void decline_afterApprove_succeeds() {
        // given
        final CourseProposal proposal = newProposal();
        proposal.approve();

        // when
        proposal.decline();

        // then
        assertThat(proposal.toDTO().status()).isEqualTo(CourseProposalStatusDTO.DECLINED);
    }

    private CourseProposal newProposal() {
        return new CourseProposal(new CreateCourseProposalCommand(UUID_VALUE));
    }
}
