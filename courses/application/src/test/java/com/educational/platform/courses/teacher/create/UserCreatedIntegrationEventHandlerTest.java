package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("newteacher", "newteacher@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand createTeacherCommand = argument.getValue();
        assertThat(createTeacherCommand)
                .hasFieldOrPropertyWithValue("username", "newteacher");
    }

    @Test
    void handleUserCreatedEvent_emptyUsername_propagatedToCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("", "newteacher@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand createTeacherCommand = argument.getValue();
        assertThat(createTeacherCommand)
                .hasFieldOrPropertyWithValue("username", "");
    }

    @Test
    void handleUserCreatedEvent_nullUsername_propagatedToCommand() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(null, "newteacher@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        final CreateTeacherCommand createTeacherCommand = argument.getValue();
        assertThat(createTeacherCommand)
                .hasFieldOrPropertyWithValue("username", null);
    }

    @Test
    void handleUserCreatedEvent_emailIgnored_onlyUsernameMapped() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("newteacher", "ignored@test.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "newteacher");
    }

    @Test
    void handleUserCreatedEvent_commandHandlerThrows_exceptionPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("newteacher", "newteacher@test.com");
        doThrow(new RuntimeException("teacher could not be created"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when / then
        assertThatThrownBy(() -> sut.handleUserCreatedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("teacher could not be created");
    }

}
