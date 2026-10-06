package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class StudentUserCreatedIntegrationEventHandlerTest {

    @Mock
    private CreateStudentCommandHandler createStudentCommandHandler;

    @InjectMocks
    private StudentUserCreatedIntegrationEventHandler sut;

    @Test
    void handleUserCreatedEvent_delegatesWithUsername() {
        // when
        sut.handleUserCreatedEvent(new UserCreatedIntegrationEvent("username", "username@example.com"));

        // then
        verify(createStudentCommandHandler).handle(new CreateStudentCommand("username"));
        verifyNoMoreInteractions(createStudentCommandHandler);
    }
}
