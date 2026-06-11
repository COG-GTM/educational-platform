package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    private UserCreatedIntegrationEventHandler sut;

    @BeforeEach
    void setUp() {
        sut = new UserCreatedIntegrationEventHandler(createTeacherCommandHandler);
    }

    @Test
    void handleUserCreatedEvent_delegatesToCreateTeacherHandlerWithUsername() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> captor = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler).handle(captor.capture());
        assertThat(captor.getValue().username()).isEqualTo("username");
    }
}
