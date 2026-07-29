package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link UserCreatedIntegrationEvent}.
 */
@Component
public class UserCreatedIntegrationEventHandler {

    private final CreateStudentCommandHandler createStudentCommandHandler;

    public UserCreatedIntegrationEventHandler(CreateStudentCommandHandler createStudentCommandHandler) {
        this.createStudentCommandHandler = createStudentCommandHandler;
    }

    @Async
    @EventListener
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        createStudentCommandHandler.handle(new CreateStudentCommand(event.uuid(), event.username()));
    }

}
