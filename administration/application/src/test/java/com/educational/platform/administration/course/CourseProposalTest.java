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
    void create_validCommand_versionNotInitializedByDomain() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CreateCourseProposalCommand command = new CreateCourseProposalCommand(uuid);

        // when
        final CourseProposal courseProposal = new CourseProposal(command);

        // then - the @Version field is owned by the persistence provider and stays null until persisted
        assertThat(courseProposal).hasFieldOrPropertyWithValue("version", null);
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
    void approve_validCommand_versionNotManagedByDomain() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));

        // when - a domain mutation occurs
        proposal.approve();

        // then - the @Version field is owned by the persistence provider and is never touched by domain logic
        assertThat(proposal).hasFieldOrPropertyWithValue("version", null);
    }

    @Test
    void approve_versionAlreadyPopulated_versionLeftUntouchedByDomain() {
        // given - a proposal whose @Version has already been populated by the persistence provider
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        ReflectionTestUtils.setField(proposal, "version", 5);

        // when - a domain mutation occurs
        proposal.approve();

        // then - approving flips the status but must not read, reset or otherwise manage the @Version;
        // the optimistic-lock counter is owned exclusively by JPA and only advances on a persisted write
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED)
                .hasFieldOrPropertyWithValue("version", 5);
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
    void decline_validCommand_versionNotManagedByDomain() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));

        // when - a domain mutation occurs
        proposal.decline();

        // then - the @Version field is owned by the persistence provider and is never touched by domain logic
        assertThat(proposal).hasFieldOrPropertyWithValue("version", null);
    }

    @Test
    void decline_versionAlreadyPopulated_versionLeftUntouchedByDomain() {
        // given - a proposal whose @Version has already been populated by the persistence provider
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal proposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        ReflectionTestUtils.setField(proposal, "version", 5);

        // when - a domain mutation occurs
        proposal.decline();

        // then - declining flips the status but must not read, reset or otherwise manage the @Version;
        // the optimistic-lock counter is owned exclusively by JPA and only advances on a persisted write
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED)
                .hasFieldOrPropertyWithValue("version", 5);
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
    void toDTO_versionPopulated_versionExcludedFromReadModel() {
        // given - a proposal whose @Version has been populated by the persistence provider
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final CourseProposal versioned = new CourseProposal(new CreateCourseProposalCommand(uuid));
        ReflectionTestUtils.setField(versioned, "version", 7);

        // when
        final CourseProposalDTO dto = versioned.toDTO();

        // then - the @Version field is a persistence concern and must not leak into the read model;
        // the DTO is identical to the one produced from an unversioned proposal with the same state
        final CourseProposalDTO unversionedDto = new CourseProposal(new CreateCourseProposalCommand(uuid)).toDTO();
        assertThat(dto)
                .isEqualTo(unversionedDto)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatusDTO.WAITING_FOR_APPROVAL);
    }

}
