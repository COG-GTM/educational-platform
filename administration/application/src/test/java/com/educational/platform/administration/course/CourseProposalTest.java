package com.educational.platform.administration.course;

import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.common.domain.AggregateRoot;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    void approve_declinedProposal_approvedStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.DECLINED);

        // when
        proposal.approve();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);
    }

    @Test
    void decline_approvedProposal_declinedStatus() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.APPROVED);

        // when
        proposal.decline();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);
    }

    @Test
    void toDTO_approvedProposal_approvedStatusInDTO() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_declinedProposal_declinedStatusInDTO() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void toDTO_approvedAfterDeclined_approvedStatusInDTO() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.DECLINED);
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void toDTO_declinedAfterApproved_declinedStatusInDTO() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.APPROVED);
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void approve_thenApproveAgain_throwsAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        proposal.approve();

        // when
        final ThrowableAssert.ThrowingCallable secondApprove = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class).isThrownBy(secondApprove);
    }

    @Test
    void decline_thenDeclineAgain_throwsAlreadyDeclinedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        proposal.decline();

        // when
        final ThrowableAssert.ThrowingCallable secondDecline = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class).isThrownBy(secondDecline);
    }

    @Test
    void create_nullUuid_createsProposalWithNullUuid() {
        // given
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(null);

        // when
        final CourseProposal proposal = new CourseProposal(command);

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("uuid", null)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void toDTO_nullUuid_dtoContainsNullUuid() {
        // given
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(null);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto.uuid()).isNull();
        assertThat(dto.status()).isEqualTo(CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

    @Test
    void approve_alreadyApproved_exceptionContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        proposal.approve();

        // when
        final ThrowableAssert.ThrowingCallable secondApprove = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class)
                .isThrownBy(secondApprove)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void decline_alreadyDeclined_exceptionContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(createCourseProposalCommand);
        proposal.decline();

        // when
        final ThrowableAssert.ThrowingCallable secondDecline = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class)
                .isThrownBy(secondDecline)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void implementsAggregateRoot() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        final CourseProposal proposal = new CourseProposal(command);

        // then
        assertThat(proposal).isInstanceOf(AggregateRoot.class);
    }

    @Test
    void class_hasEntityAnnotation() {
        // then
        assertThat(CourseProposal.class.isAnnotationPresent(jakarta.persistence.Entity.class)).isTrue();
    }

    @Test
    void toDTO_calledTwice_returnsEqualResults() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        final CourseProposalDTO dto1 = proposal.toDTO();
        final CourseProposalDTO dto2 = proposal.toDTO();

        // then
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    void approve_waitingForApproval_statusIsApprovedNotDeclined() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        proposal.approve();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);
        assertThat(proposal)
                .extracting("status")
                .isNotEqualTo(CourseProposalStatus.DECLINED)
                .isNotEqualTo(CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void decline_waitingForApproval_statusIsDeclinedNotApproved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        proposal.decline();

        // then
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);
        assertThat(proposal)
                .extracting("status")
                .isNotEqualTo(CourseProposalStatus.APPROVED)
                .isNotEqualTo(CourseProposalStatus.WAITING_FOR_APPROVAL);
    }

    @Test
    void statusField_hasEnumeratedStringAnnotation() throws NoSuchFieldException {
        // when
        final var field = CourseProposal.class.getDeclaredField("status");

        // then
        assertThat(field.isAnnotationPresent(jakarta.persistence.Enumerated.class)).isTrue();
        assertThat(field.getAnnotation(jakarta.persistence.Enumerated.class).value())
                .isEqualTo(jakarta.persistence.EnumType.STRING);
    }

    @Test
    void idField_hasIdAnnotation() throws NoSuchFieldException {
        // when
        final var field = CourseProposal.class.getDeclaredField("id");

        // then
        assertThat(field.isAnnotationPresent(jakarta.persistence.Id.class)).isTrue();
    }

    @Test
    void idField_hasGeneratedValueIdentityStrategy() throws NoSuchFieldException {
        // when
        final var field = CourseProposal.class.getDeclaredField("id");

        // then
        assertThat(field.isAnnotationPresent(jakarta.persistence.GeneratedValue.class)).isTrue();
        assertThat(field.getAnnotation(jakarta.persistence.GeneratedValue.class).strategy())
                .isEqualTo(jakarta.persistence.GenerationType.IDENTITY);
    }

    @Test
    void jpaNoArgConstructor_exists() {
        // then
        assertThat(CourseProposal.class.getDeclaredConstructors())
                .anyMatch(c -> c.getParameterCount() == 0);
    }

    @Test
    void fullLifecycle_approveDeclineApprove_correctStatesAtEachStep() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // initial state
        assertThat(proposal).hasFieldOrPropertyWithValue("status", CourseProposalStatus.WAITING_FOR_APPROVAL);

        // first approve
        proposal.approve();
        assertThat(proposal).hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);

        // then decline
        proposal.decline();
        assertThat(proposal).hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);

        // then approve again
        proposal.approve();
        assertThat(proposal).hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);
    }

    @Test
    void approve_preservesUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        proposal.approve();

        // then
        assertThat(proposal).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void decline_preservesUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        proposal.decline();

        // then
        assertThat(proposal).hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void toDTO_afterFullLifecycle_reflectsLatestState() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);
        proposal.approve();
        proposal.decline();
        proposal.approve();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.APPROVED);
    }

    @Test
    void approve_nullUuid_thenApproveAgain_throwsAlreadyApprovedWithNullInMessage() {
        // given
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(null);
        final CourseProposal proposal = new CourseProposal(command);
        proposal.approve();

        // when
        final ThrowableAssert.ThrowingCallable secondApprove = proposal::approve;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class)
                .isThrownBy(secondApprove)
                .withMessageContaining("null");
    }

    @Test
    void decline_nullUuid_thenDeclineAgain_throwsAlreadyDeclinedWithNullInMessage() {
        // given
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(null);
        final CourseProposal proposal = new CourseProposal(command);
        proposal.decline();

        // when
        final ThrowableAssert.ThrowingCallable secondDecline = proposal::decline;

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class)
                .isThrownBy(secondDecline)
                .withMessageContaining("null");
    }

    @Test
    void create_newProposal_idFieldIsNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        final CourseProposal proposal = new CourseProposal(command);

        // then
        assertThat(proposal).hasFieldOrPropertyWithValue("id", null);
    }

    @Test
    void toDTO_afterApproveFromDeclined_thenDeclineAgain_reflectsDeclined() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.DECLINED);
        proposal.approve();
        proposal.decline();

        // when
        final CourseProposalDTO dto = proposal.toDTO();

        // then
        assertThat(dto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.DECLINED);
    }

    @Test
    void approve_fromWaitingForApproval_doesNotThrow() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when / then
        assertThatCode(proposal::approve).doesNotThrowAnyException();
    }

    @Test
    void decline_fromWaitingForApproval_doesNotThrow() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when / then
        assertThatCode(proposal::decline).doesNotThrowAnyException();
    }

    @Test
    void approve_fromDeclined_doesNotThrow() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.DECLINED);

        // when / then
        assertThatCode(proposal::approve).doesNotThrowAnyException();
    }

    @Test
    void decline_fromApproved_doesNotThrow() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);
        ReflectionTestUtils.setField(proposal, "status", CourseProposalStatus.APPROVED);

        // when / then
        assertThatCode(proposal::decline).doesNotThrowAnyException();
    }

    @Test
    void toDTO_calledTwiceAfterApprove_returnsEqualResults() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);
        proposal.approve();

        // when
        final CourseProposalDTO dto1 = proposal.toDTO();
        final CourseProposalDTO dto2 = proposal.toDTO();

        // then
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    void toDTO_calledTwiceAfterDecline_returnsEqualResults() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);
        proposal.decline();

        // when
        final CourseProposalDTO dto1 = proposal.toDTO();
        final CourseProposalDTO dto2 = proposal.toDTO();

        // then
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    void toDTO_calledTwice_returnsDifferentInstances() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);
        final CourseProposal proposal = new CourseProposal(command);

        // when
        final CourseProposalDTO dto1 = proposal.toDTO();
        final CourseProposalDTO dto2 = proposal.toDTO();

        // then
        assertThat(dto1).isNotSameAs(dto2);
    }

}
