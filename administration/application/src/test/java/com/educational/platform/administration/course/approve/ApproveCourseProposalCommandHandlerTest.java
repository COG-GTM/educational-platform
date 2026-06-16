package com.educational.platform.administration.course.approve;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalAlreadyApprovedException;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.exception.ResourceNotFoundException;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApproveCourseProposalCommandHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ApproveCourseProposalCommandHandler sut;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        sut = new ApproveCourseProposalCommandHandler(transactionTemplate, repository, eventPublisher);
    }

    @Test
    void handle_existingCourseProposal_courseProposalSavedWithStatusApproved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        final CourseProposal proposal = argument.getValue();
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);

        final ArgumentCaptor<CourseApprovedByAdminIntegrationEvent> eventArgument = ArgumentCaptor.forClass(CourseApprovedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final CourseApprovedByAdminIntegrationEvent event = eventArgument.getValue();
        assertThat(event)
                .hasFieldOrPropertyWithValue("courseId", uuid);
    }

    @Test
    void handle_invalidId_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_invalidId_nothingSavedAndNoEventPublished() {
        // given - the proposal does not exist, so the command fails before any mutation
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - completes the failure-mode contract alongside the optimistic-lock and domain-guard
        // cases: a failed command must neither persist the aggregate nor emit an integration event
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_optimisticLockingConflictOnSave_exceptionPropagatedAndNoEventPublished() {
        // given - the proposal is loaded but a concurrent admin already bumped the @Version,
        // so the save inside the transaction fails the optimistic-lock check
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CourseProposal correspondingCourseProposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));
        when(repository.save(any(CourseProposal.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(CourseProposal.class, uuid));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - the conflict surfaces to the caller and no downstream integration event is emitted
        assertThatExceptionOfType(ObjectOptimisticLockingFailureException.class).isThrownBy(handle);
        verifyNoInteractions(eventPublisher);

        // and - the failure happens at the persistence write of an already-approved aggregate,
        // confirming it is the @Version check (not a domain guard) that rejects the concurrent edit
        final ArgumentCaptor<CourseProposal> savedProposal = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(savedProposal.capture());
        assertThat(savedProposal.getValue())
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.APPROVED);
    }

    @Test
    void handle_alreadyApprovedProposal_domainGuardRejectsWithoutSavingOrPublishing() {
        // given - the loaded aggregate is already APPROVED, so the domain guard (not the @Version
        // check) rejects the duplicate approval; companion to the optimistic-lock conflict case
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CourseProposal alreadyApproved = new CourseProposal(new CreateCourseProposalCommand(uuid));
        alreadyApproved.approve();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(alreadyApproved));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then - the conflict surfaces and the failed command neither persists nor emits an event
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class).isThrownBy(handle);
        verify(repository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_existingCourseProposal_eventPublishedOnlyAfterVersionedSave() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CourseProposal correspondingCourseProposal = new CourseProposal(new CreateCourseProposalCommand(uuid));
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then - the integration event must be emitted strictly AFTER the version-checked save, so an
        // approval is never announced for a write the @Version optimistic-lock check could still reject
        final InOrder inOrder = inOrder(repository, eventPublisher);
        inOrder.verify(repository).save(any(CourseProposal.class));
        inOrder.verify(eventPublisher).publishEvent(any(CourseApprovedByAdminIntegrationEvent.class));
    }
}
