package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedEventRepository;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand command = argument.getValue();
        assertThat(command).hasFieldOrPropertyWithValue("username", "testuser");
    }

    @Test
    void handleUserCreatedEvent_transientException_rethrown() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(createTeacherCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void handleUserCreatedEvent_businessException_rethrown() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        doThrow(new ResourceNotFoundException("User not found"))
                .when(createTeacherCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void handleUserCreatedEvent_pessimisticLockingException_rethrown() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        doThrow(new PessimisticLockingFailureException("pessimistic lock"))
                .when(createTeacherCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(PessimisticLockingFailureException.class);
    }

    @Test
    void handleUserCreatedEvent_successfulHandling_doesNotPersistFailedEvent() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void recover_persistsFailedEvent() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException("DB connection lost");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
        assertThat(failedEvent.getEventPayload()).isEqualTo(event.toString());
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("DB connection lost");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void recover_withNullExceptionMessage_usesExceptionClassName() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        final OptimisticLockingFailureException exception = new OptimisticLockingFailureException(null);

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue().getExceptionMessage()).isEqualTo(OptimisticLockingFailureException.class.getName());
    }

    @Test
    void recover_withPessimisticLockingException_persistsFailedEvent() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        final PessimisticLockingFailureException exception = new PessimisticLockingFailureException("deadlock detected");

        // when
        sut.recover(exception, event);

        // then
        final ArgumentCaptor<FailedIntegrationEventRecord> argument = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(argument.capture());
        final FailedIntegrationEventRecord failedEvent = argument.getValue();
        assertThat(failedEvent.getExceptionMessage()).isEqualTo("deadlock detected");
        assertThat(failedEvent.getRetryCount()).isEqualTo(3);
        assertThat(failedEvent.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void handleUserCreatedEvent_genericRuntimeException_rethrown() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        doThrow(new IllegalStateException("invalid state"))
                .when(createTeacherCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("invalid state");
    }

    @Test
    void handleUserCreatedEvent_transientException_preservesOriginalMessage() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        doThrow(new OptimisticLockingFailureException("specific DB error"))
                .when(createTeacherCommandHandler).handle(any());

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("specific DB error");
    }

    @Test
    void handleUserCreatedEvent_transientException_doesNotPersistFailedEvent() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("testuser", "test@example.com");
        doThrow(new OptimisticLockingFailureException("DB connection lost"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        try {
            sut.handleUserCreatedEvent(event);
        } catch (OptimisticLockingFailureException ignored) {
        }

        // then
        verifyNoInteractions(failedEventRepository);
    }

}
