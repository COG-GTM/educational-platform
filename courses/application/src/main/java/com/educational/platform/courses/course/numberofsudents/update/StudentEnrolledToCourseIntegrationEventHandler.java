package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link StudentEnrolledToCourseIntegrationEvent}. Executed asynchronously
 * after the publishing transaction commits, with retry on failure.
 */
@Component
public class StudentEnrolledToCourseIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(StudentEnrolledToCourseIntegrationEventHandler.class);

    private final IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    public StudentEnrolledToCourseIntegrationEventHandler(IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler) {
        this.increaseNumberOfStudentsCommandHandler = increaseNumberOfStudentsCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(event.courseId()));
    }

    @Recover
    void recover(Exception exception, StudentEnrolledToCourseIntegrationEvent event) {
        log.error("Failed to process {} after retries, event will be dropped: {}",
                event.getClass().getSimpleName(), event, exception);
    }

}
