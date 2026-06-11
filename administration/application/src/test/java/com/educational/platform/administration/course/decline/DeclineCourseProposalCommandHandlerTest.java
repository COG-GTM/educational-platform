package com.educational.platform.administration.course.decline;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalAlreadyDeclinedException;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.CourseProposalStatus;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent;
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
public class DeclineCourseProposalCommandHandlerTest {

    @Mock
    private CourseProposalRepository repository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeclineCourseProposalCommandHandler sut;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        sut = new DeclineCourseProposalCommandHandler(transactionTemplate, repository, eventPublisher);
    }

    @Test
    void handle_existingCourseProposal_courseProposalSavedWithStatusDeclined() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

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
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);

        final ArgumentCaptor<CourseDeclinedByAdminIntegrationEvent> eventArgument = ArgumentCaptor.forClass(CourseDeclinedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final CourseDeclinedByAdminIntegrationEvent event = eventArgument.getValue();
        assertThat(event)
                .hasFieldOrPropertyWithValue("courseId", uuid);
    }

    @Test
    void handle_invalidId_resourceNotFoundException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(handle);
    }

    @Test
    void handle_alreadyDeclinedProposal_throwsAlreadyDeclinedException() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.DECLINED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class).isThrownBy(handle);
    }

    @Test
    void handle_alreadyDeclinedProposal_eventNotPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.DECLINED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        try {
            sut.handle(command);
        } catch (CourseProposalAlreadyDeclinedException ignored) {
        }

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void handle_alreadyDeclinedProposal_proposalNotSaved() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.DECLINED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        try {
            sut.handle(command);
        } catch (CourseProposalAlreadyDeclinedException ignored) {
        }

        // then
        verify(repository, never()).save(any());
    }

    @Test
    void handle_invalidId_eventNotPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);
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
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);
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
    void handle_approvedProposal_savedWithDeclinedStatusAndEventPublished() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.APPROVED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        final CourseProposal proposal = argument.getValue();
        assertThat(proposal)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);

        final ArgumentCaptor<CourseDeclinedByAdminIntegrationEvent> eventArgument = ArgumentCaptor.forClass(CourseDeclinedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        final CourseDeclinedByAdminIntegrationEvent event = eventArgument.getValue();
        assertThat(event)
                .hasFieldOrPropertyWithValue("courseId", uuid);
    }

    @Test
    void handle_invalidId_exceptionMessageContainsUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);
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
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

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
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        verify(eventPublisher, times(1)).publishEvent(any(CourseDeclinedByAdminIntegrationEvent.class));
    }

    @Test
    void handle_hasPreAuthorizeAnnotation() throws NoSuchMethodException {
        // when
        final var method = DeclineCourseProposalCommandHandler.class
                .getMethod("handle", DeclineCourseProposalCommand.class);

        // then
        assertThat(method.isAnnotationPresent(org.springframework.security.access.prepost.PreAuthorize.class)).isTrue();
    }

    @Test
    void handle_preAuthorizeAnnotation_requiresAdminRole() throws NoSuchMethodException {
        // when
        final var method = DeclineCourseProposalCommandHandler.class
                .getMethod("handle", DeclineCourseProposalCommand.class);
        final org.springframework.security.access.prepost.PreAuthorize annotation =
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        // then
        assertThat(annotation.value()).contains("hasRole('ADMIN')");
    }

    @Test
    void class_hasNamedAnnotation() {
        assertThat(DeclineCourseProposalCommandHandler.class.isAnnotationPresent(jakarta.inject.Named.class)).isTrue();
    }

    @Test
    void handle_existingCourseProposal_repositoryQueriedWithCommandUuid() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

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
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseDeclinedByAdminIntegrationEvent> eventArgument = ArgumentCaptor.forClass(CourseDeclinedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        assertThat(eventArgument.getValue().courseId()).isEqualTo(command.uuid());
    }

    @Test
    void handle_preAuthorizeAnnotation_exactValue() throws NoSuchMethodException {
        // when
        final var method = DeclineCourseProposalCommandHandler.class
                .getMethod("handle", DeclineCourseProposalCommand.class);
        final org.springframework.security.access.prepost.PreAuthorize annotation =
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        // then
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    @Test
    void handle_existingCourseProposal_savedProposalIsSameInstanceFromRepository() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

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
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

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
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

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

    @Test
    void class_doesNotHaveTransactionalAnnotation() {
        assertThat(DeclineCourseProposalCommandHandler.class
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
    }

    @Test
    void constructor_acceptsTransactionTemplateRepositoryAndEventPublisher() throws NoSuchMethodException {
        // when
        final var constructor = DeclineCourseProposalCommandHandler.class.getConstructor(
                org.springframework.transaction.support.TransactionTemplate.class,
                CourseProposalRepository.class,
                org.springframework.context.ApplicationEventPublisher.class
        );

        // then
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterCount()).isEqualTo(3);
    }

    @Test
    void handle_eventPublisherThrows_exceptionPropagatesAndSaveWasStillCalled() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));
        doThrow(new RuntimeException("event publish failure")).when(eventPublisher).publishEvent(any(CourseDeclinedByAdminIntegrationEvent.class));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle)
                .withMessageContaining("event publish failure");
        verify(repository).save(any(CourseProposal.class));
    }

    @Test
    void handle_existingCourseProposal_publishedEventTypeIsCourseDeclined() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<Object> eventArgument = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        assertThat(eventArgument.getValue()).isInstanceOf(CourseDeclinedByAdminIntegrationEvent.class);
        assertThat(eventArgument.getValue()).isNotInstanceOf(
                com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent.class);
    }

    @Test
    void handle_existingCourseProposal_savedProposalUuidMatchesCommand() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("uuid", uuid);
    }

    @Test
    void handle_existingCourseProposal_saveCalledBeforeEventPublish() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final var inOrder = inOrder(repository, eventPublisher);
        inOrder.verify(repository).save(any(CourseProposal.class));
        inOrder.verify(eventPublisher).publishEvent(any(CourseDeclinedByAdminIntegrationEvent.class));
    }

    @Test
    void handle_alreadyDeclinedProposal_repositoryFindByUuidStillCalled() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.DECLINED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        try {
            sut.handle(command);
        } catch (CourseProposalAlreadyDeclinedException ignored) {
        }

        // then
        verify(repository).findByUuid(uuid);
    }

    @Test
    void handle_invalidId_exceptionMessageMatchesExpectedFormat() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(handle)
                .withMessage("Course Proposal with uuid: " + uuid + " not found");
    }

    @Test
    void handle_nilUuid_proposalFound_savedWithDeclinedStatus() {
        // given
        final UUID nilUuid = new UUID(0L, 0L);
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(nilUuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(nilUuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", nilUuid);
        when(repository.findByUuid(nilUuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseProposal> argument = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED)
                .hasFieldOrPropertyWithValue("uuid", nilUuid);
    }

    @Test
    void handle_existingCourseProposal_publishedEventUuidNotNull() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<CourseDeclinedByAdminIntegrationEvent> eventArgument = ArgumentCaptor.forClass(CourseDeclinedByAdminIntegrationEvent.class);
        verify(eventPublisher).publishEvent(eventArgument.capture());
        assertThat(eventArgument.getValue().courseId()).isNotNull();
    }

    @Test
    void handle_alreadyDeclinedProposal_exceptionMessageMatchesExpectedFormat() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        ReflectionTestUtils.setField(correspondingCourseProposal, "status", CourseProposalStatus.DECLINED);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handle(command);

        // then
        assertThatExceptionOfType(CourseProposalAlreadyDeclinedException.class)
                .isThrownBy(handle)
                .withMessage("Course Proposal with uuid = " + uuid + " cannot be declined, course proposal was already declined");
    }

    @Test
    void handle_existingCourseProposal_commandUuidPassedToRepository() {
        // given
        final UUID uuid = UUID.fromString("223e4567-e89b-12d3-a456-426655440099");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        // when
        sut.handle(command);

        // then
        final ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(repository).findByUuid(uuidCaptor.capture());
        assertThat(uuidCaptor.getValue()).isEqualTo(uuid);
    }

    @Test
    void handle_multipleSequentialCommands_eachProcessedIndependently() {
        // given
        final UUID uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final DeclineCourseProposalCommand command1 = new DeclineCourseProposalCommand(uuid1);
        final DeclineCourseProposalCommand command2 = new DeclineCourseProposalCommand(uuid2);

        final CourseProposal proposal1 = new CourseProposal(new CreateCourseProposalCommand(uuid1));
        ReflectionTestUtils.setField(proposal1, "uuid", uuid1);
        when(repository.findByUuid(uuid1)).thenReturn(Optional.of(proposal1));

        final CourseProposal proposal2 = new CourseProposal(new CreateCourseProposalCommand(uuid2));
        ReflectionTestUtils.setField(proposal2, "uuid", uuid2);
        when(repository.findByUuid(uuid2)).thenReturn(Optional.of(proposal2));

        // when
        sut.handle(command1);
        sut.handle(command2);

        // then
        final ArgumentCaptor<CourseProposal> saveCaptor = ArgumentCaptor.forClass(CourseProposal.class);
        verify(repository, times(2)).save(saveCaptor.capture());
        assertThat(saveCaptor.getAllValues().get(0))
                .hasFieldOrPropertyWithValue("uuid", uuid1)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);
        assertThat(saveCaptor.getAllValues().get(1))
                .hasFieldOrPropertyWithValue("uuid", uuid2)
                .hasFieldOrPropertyWithValue("status", CourseProposalStatus.DECLINED);

        final ArgumentCaptor<CourseDeclinedByAdminIntegrationEvent> eventCaptor = ArgumentCaptor.forClass(CourseDeclinedByAdminIntegrationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues().get(0).courseId()).isEqualTo(uuid1);
        assertThat(eventCaptor.getAllValues().get(1).courseId()).isEqualTo(uuid2);
    }
}
