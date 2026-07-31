package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateStudentCommandHandler createStudentCommandHandler;

    @InjectMocks
    private UserCreatedIntegrationEventHandler sut;


    @Test
    void handleUserCreatedEvent_createStudentCommandExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440101");
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent(uuid, "username", "email@gmail.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateStudentCommand> argument = ArgumentCaptor.forClass(CreateStudentCommand.class);
        verify(createStudentCommandHandler).handle(argument.capture());
        final CreateStudentCommand command = argument.getValue();
        assertThat(command)
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("username", "username");
    }
}
