package com.educational.platform.administration.course;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CourseProposalTest {

    @Test
    void create_validCommand_created() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        final CourseProposal courseProposal = new CourseProposal(command);

        // then
        assertThat(courseProposal)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void approve_approvedStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);

        // when
        proposal.approve();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);
    }

    @Test
    void approve_courseProposalAlreadyApproved_courseProposalAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(proposal, "id", 11);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.APPROVED);

        // when
        final ThrowableAssert.ThrowingCallable sendToApprove = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class).isThrownBy(sendToApprove);
    }

    @Test
    void decline_declinedStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);

        // when
        proposal.decline();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);
    }

    @Test
    void decline_courseProposalAlreadyDeclined_courseProposalAlreadyDeclinedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(proposal, "id", 11);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.DECLINED);

        // when
        final ThrowableAssert.ThrowingCallable sendToDecline = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class).isThrownBy(sendToDecline);
    }

    @Test
    void approve_declinedProposal_approvedStatus() {
        // given - the approve guard only blocks an already-approved proposal, so a declined proposal can still be approved
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
    void decline_approvedProposal_declinedStatus() {
        // given - the decline guard only blocks an already-declined proposal, so an approved proposal can still be declined
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
    void toDTO_correspondingDTOCreated() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_approvedProposal_statusMappedToApproved() {
        // given - the approve command flow calls toDTO() after approving, so the APPROVED status must map to its DTO
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_declinedProposal_statusMappedToDeclined() {
        // given - the decline command flow calls toDTO() after declining, so the DECLINED status must map to its DTO
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void approve_courseProposalAlreadyApproved_exceptionMessageIdentifiesProposal() {
        // given - the already-approved guard message embeds the proposal uuid for traceability across the approve flow
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440021");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.APPROVED);

        // when
        final ThrowableAssert.ThrowingCallable approve = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class)
                .isThrownBy(approve)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void decline_courseProposalAlreadyDeclined_exceptionMessageIdentifiesProposal() {
        // given - the already-declined guard message embeds the proposal uuid for traceability across the decline flow
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440022");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.DECLINED);

        // when
        final ThrowableAssert.ThrowingCallable decline = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class)
                .isThrownBy(decline)
                .withMessageContaining(uuid.toString());
    }

}
