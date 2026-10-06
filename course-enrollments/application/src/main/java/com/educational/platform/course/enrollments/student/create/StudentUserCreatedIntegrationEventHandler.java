package com.educational.platform.course.enrollments.student.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link UserCreatedIntegrationEvent}, registers the user as a student in the enrollments module.
 */
@Component
public class StudentUserCreatedIntegrationEventHandler {

    private final CreateStudentCommandHandler createStudentCommandHandler;

    public StudentUserCreatedIntegrationEventHandler(CreateStudentCommandHandler createStudentCommandHandler) {
        this.createStudentCommandHandler = createStudentCommandHandler;
    }

    @Async
    @EventListener
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        createStudentCommandHandler.handle(new CreateStudentCommand(event.username()));
    }

}
