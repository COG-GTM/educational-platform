package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCreatedIntegrationEventHandlerEdgeCaseTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_validEvent_commandContainsCorrectUsername() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher-user", "teacher@example.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(argument.capture());
        assertThat(argument.getValue().username()).isEqualTo("teacher-user");
    }

    @Test
    void handleUserCreatedEvent_handlerThrowsException_exceptionPropagated() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher-user", "teacher@example.com");
        doThrow(new RuntimeException("Database error"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        final ThrowableAssert.ThrowingCallable handle = () -> sut.handleUserCreatedEvent(event);

        // then
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(handle);
    }
}
