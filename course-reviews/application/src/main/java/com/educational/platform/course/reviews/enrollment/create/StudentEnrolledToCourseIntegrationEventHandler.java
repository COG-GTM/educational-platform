package com.educational.platform.course.reviews.enrollment.create;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link StudentEnrolledToCourseIntegrationEvent}. The enrollment projection is authorization data,
 * so it is only written once the publishing enrollment transaction has committed.
 */
@Component
public class StudentEnrolledToCourseIntegrationEventHandler {

    private final CreateReviewerEnrollmentCommandHandler createReviewerEnrollmentCommandHandler;

    public StudentEnrolledToCourseIntegrationEventHandler(CreateReviewerEnrollmentCommandHandler createReviewerEnrollmentCommandHandler) {
        this.createReviewerEnrollmentCommandHandler = createReviewerEnrollmentCommandHandler;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        createReviewerEnrollmentCommandHandler.handle(new CreateReviewerEnrollmentCommand(event.courseId(), event.username()));
    }

}
