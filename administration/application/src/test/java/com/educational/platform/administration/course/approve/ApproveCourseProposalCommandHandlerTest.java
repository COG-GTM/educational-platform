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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;
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
    void handle_alreadyApprovedProposal_throwsAlreadyApprovedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseProposalAlreadyApprovedException.class).isThrownBy(handle);
    }

    @Test
    void handle_alreadyApprovedProposal_eventNotPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        try {
            sut.handle(command);
        } catch (CourseProposalAlreadyApprovedException ignored) {
        }

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_alreadyApprovedProposal_proposalNotSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        try {
            sut.handle(command);
        } catch (CourseProposalAlreadyApprovedException ignored) {
        }

        // then
        verify(repository, never()).save(any());
    }

    @Test
    void handle_invalidId_eventNotPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        try {
            sut.handle(command);
        } catch (ResourceNotFoundException ignored) {
        }

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_invalidId_proposalNotSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        try {
            sut.handle(command);
        } catch (ResourceNotFoundException ignored) {
        }

        // then
        verify(repository, never()).save(any());
    }

    @Test
    void handle_declinedProposal_savedWithApprovedStatusAndEventPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.DECLINED);
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
    void handle_invalidId_exceptionMessageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(handle)
                .withMessageContaining(uuid.toString());
    }

    @Test
    void handle_existingCourseProposal_repositorySaveCalledExactlyOnce() {
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
        verify(repository, times(1)).save(any(CourseProposal.class));
    }

    @Test
    void handle_existingCourseProposal_eventPublishedExactlyOnce() {
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
        verify(eventPublisher, times(1)).publishEvent(any(CourseApprovedByAdminIntegrationEvent.class));
    }

    @Test
    void handle_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        // when
        final var method = ApproveCourseProposalCommandHandler.class
                .getMethod("handle", ApproveCourseProposalCommand.class);

        // then
        assertThat(method.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class)).isTrue();
    }

    @Test
    void handle_preAuthorizeAnnotation_requiresAdminRole() throws NoSuchMethodException {
        // when
        final var method = ApproveCourseProposalCommandHandler.class
                .getMethod("handle", ApproveCourseProposalCommand.class);
        final org.springframework.security.access.prepost.PreAuthorize annotation =
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        // then
        assertThat(annotation.value()).contains("hasRole('ADMIN')");
    }

    @Test
    void class_hasNamedAnnotation() {
        assertThat(ApproveCourseProposalCommandHandler.class.isAnnotationPresent(jakarta.inject.Named.class)).isTrue();
    }

    @Test
    void handle_existingCourseProposal_repositoryQueriedWithCommandUuid() {
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
        verify(repository).findByUuid(uuid);
    }

    @Test
    void handle_existingCourseProposal_publishedEventUuidMatchesCommandUuid() {
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
        final ArgumentCaptor<CourseApprovedByAdminIntegrationEvent> eventArgument = ArgumentCaptor.forClass(CourseApprovedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        assertThat(eventArgument.getValue().courseId()).isEqualTo(command.uuid());
    }

    @Test
    void handle_preAuthorizeAnnotation_exactValue() throws NoSuchMethodException {
        // when
        final var method = ApproveCourseProposalCommandHandler.class
                .getMethod("handle", ApproveCourseProposalCommand.class);
        final org.springframework.security.access.prepost.PreAuthorize annotation =
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        // then
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void handle_existingCourseProposal_savedProposalIsSameInstanceFromRepository() {
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
        assertThat(argument.getValue()).isSameAs(correspondingCourseProposal);
    }

    @Test
    void handle_existingCourseProposal_noMoreRepositoryInteractions() {
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
        verify(repository).findByUuid(uuid);
        verify(repository).save(any(CourseProposal.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void handle_repositoryThrowsOnSave_exceptionPropagatesAndEventNotPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));
        doThrow(new RuntimeException("DB save error")).when(repository).save(any(CourseProposal.class));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle)
                .withMessageContaining("DB save error");
        verifyNoInteractions(eventPublisher);
    }
}
