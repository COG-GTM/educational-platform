package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link StudentEnrolledToCourseIntegrationEvent}.
 */
@Component
public class StudentEnrolledToCourseIntegrationEventHandler {

    private final StudentEnrolledToCourseRetryableInvoker studentEnrolledToCourseRetryableInvoker;

    public StudentEnrolledToCourseIntegrationEventHandler(StudentEnrolledToCourseRetryableInvoker studentEnrolledToCourseRetryableInvoker) {
        this.studentEnrolledToCourseRetryableInvoker = studentEnrolledToCourseRetryableInvoker;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentEnrolledToCourseEvent(StudentEnrolledToCourseIntegrationEvent event) {
        studentEnrolledToCourseRetryableInvoker.invoke(event);
    }

}
