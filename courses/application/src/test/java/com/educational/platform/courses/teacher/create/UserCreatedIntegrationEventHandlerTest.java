package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand createTeacherCommand = argument.getValue();
        assertThat(createTeacherCommand)
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handleUserCreatedEvent_handlerCalledExactlyOnce() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher1", "teacher1@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler).handle(org.mockito.ArgumentMatchers.any(CreateTeacherCommand.class));
        verifyNoMoreInteractions(createTeacherCommandHandler);
    }

    @Test
    void handleUserCreatedEvent_onlyUsernamePassedToCommand_emailNotIncluded() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("alice", "alice@school.edu");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand command = argument.getValue();
        assertThat(command.username()).isEqualTo("alice");
    }

    @Test
    void handleUserCreatedEvent_specialCharactersInUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("user.name+tag@org", "email@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("user.name+tag@org");
    }

    @Test
    void handleUserCreatedEvent_emptyUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("", "empty@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEmpty();
    }

    @Test
    void handleUserCreatedEvent_nullUsername_nullPreservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, "email@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isNull();
    }

    @Test
    void handleUserCreatedEvent_whitespaceOnlyUsername_preservedInCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("   ", "ws@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("   ");
    }

}
