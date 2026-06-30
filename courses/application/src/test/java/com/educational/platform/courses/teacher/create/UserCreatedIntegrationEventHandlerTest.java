package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedEventStatus;
import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Mock
    private FailedIntegrationEventRepository failedEventRepository;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEventHandler sut =
                new UserCreatedIntegrationEventHandler(createTeacherCommandHandler, failedEventRepository);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "username");
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void recover_persistsFailedIntegrationEvent() {
        // given
        final UserCreatedIntegrationEventHandler sut =
                new UserCreatedIntegrationEventHandler(createTeacherCommandHandler, failedEventRepository);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.recover(new OptimisticLockingFailureException("boom"), event);

        // then
        final ArgumentCaptor<FailedIntegrationEvent> argument = ArgumentCaptor.forClass(FailedIntegrationEvent.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("eventClassName", UserCreatedIntegrationEvent.class.getName())
                .hasFieldOrPropertyWithValue("exceptionMessage", "boom")
                .hasFieldOrPropertyWithValue("retryCount", 3);
    }

    @Test
    void handleUserCreatedEvent_whenCommandHandlerThrows_propagatesException() {
        // given
        final UserCreatedIntegrationEventHandler sut =
                new UserCreatedIntegrationEventHandler(createTeacherCommandHandler, failedEventRepository);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");
        final OptimisticLockingFailureException cause = new OptimisticLockingFailureException("boom");
        doThrow(cause).when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isSameAs(cause);
        verifyNoInteractions(failedEventRepository);
    }

    @Test
    void recover_persistsEventPayloadAndFailedStatus() {
        // given
        final UserCreatedIntegrationEventHandler sut =
                new UserCreatedIntegrationEventHandler(createTeacherCommandHandler, failedEventRepository);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.recover(new OptimisticLockingFailureException("boom"), event);

        // then
        final ArgumentCaptor<FailedIntegrationEvent> argument = ArgumentCaptor.forClass(FailedIntegrationEvent.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("eventPayload", String.valueOf(event))
                .hasFieldOrPropertyWithValue("status", FailedEventStatus.FAILED);
    }

    @Test
    void recover_withNullExceptionMessage_persistsNullMessage() {
        // given
        final UserCreatedIntegrationEventHandler sut =
                new UserCreatedIntegrationEventHandler(createTeacherCommandHandler, failedEventRepository);
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.recover(new OptimisticLockingFailureException(null), event);

        // then
        final ArgumentCaptor<FailedIntegrationEvent> argument = ArgumentCaptor.forClass(FailedIntegrationEvent.class);
        verify(failedEventRepository).save(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("exceptionMessage", null)
                .hasFieldOrPropertyWithValue("eventClassName", UserCreatedIntegrationEvent.class.getName());
    }
}
