package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void handleUserCreatedEvent_subscribedToUserCreatedEventAsynchronously() throws NoSuchMethodException {
        // when
        final Method handler = StudentUserCreatedIntegrationEventHandler.class.getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

        // then
        assertThat(handler.isAnnotationPresent(EventListener.class)).isTrue();
        assertThat(handler.isAnnotationPresent(Async.class)).isTrue();
    }
}
