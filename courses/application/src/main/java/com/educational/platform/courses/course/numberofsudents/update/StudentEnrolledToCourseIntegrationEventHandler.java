package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link StudentEnrolledToCourseIntegrationEvent}.
 */
@Component
@ConditionalOnProperty(name = "courses.in-process-handlers.enabled", havingValue = "true", matchIfMissing = true)
public class StudentEnrolledToCourseIntegrationEventHandler {

    private final IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    public StudentEnrolledToCourseIntegrationEventHandler(IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler) {
        this.increaseNumberOfStudentsCommandHandler = increaseNumberOfStudentsCommandHandler;
    }

    @Async
    @EventListener
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(event.courseId()));
    }

}
