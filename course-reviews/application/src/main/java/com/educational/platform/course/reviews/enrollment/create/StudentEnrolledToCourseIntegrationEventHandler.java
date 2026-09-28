package com.educational.platform.course.reviews.enrollment.create;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link StudentEnrolledToCourseIntegrationEvent}.
 */
@Component
public class StudentEnrolledToCourseIntegrationEventHandler {

    private final CreateReviewerEnrollmentCommandHandler createReviewerEnrollmentCommandHandler;

    public StudentEnrolledToCourseIntegrationEventHandler(CreateReviewerEnrollmentCommandHandler createReviewerEnrollmentCommandHandler) {
        this.createReviewerEnrollmentCommandHandler = createReviewerEnrollmentCommandHandler;
    }

    @Async
    @EventListener
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        createReviewerEnrollmentCommandHandler.handle(new CreateReviewerEnrollmentCommand(event.courseId(), event.username()));
    }

}
