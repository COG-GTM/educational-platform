package com.educational.platform.courses.course.numberofsudents.update;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class StudentEnrolledToCourseRetryableInvoker {

    private static final Logger log = LoggerFactory.getLogger(StudentEnrolledToCourseRetryableInvoker.class);

    private final IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler;

    public StudentEnrolledToCourseRetryableInvoker(IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler) {
        this.increaseNumberOfStudentsCommandHandler = increaseNumberOfStudentsCommandHandler;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = IntegrationEventRetryPolicy.MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = IntegrationEventRetryPolicy.INITIAL_DELAY_MS,
                    multiplier = IntegrationEventRetryPolicy.MULTIPLIER,
                    maxDelay = IntegrationEventRetryPolicy.MAX_DELAY_MS))
    public void invoke(StudentEnrolledToCourseIntegrationEvent event) {
        increaseNumberOfStudentsCommandHandler.handle(new IncreaseNumberOfStudentsCommand(event.courseId()));
    }

    @Recover
    public void recover(Exception ex, StudentEnrolledToCourseIntegrationEvent event) {
        log.error("Integration event '{}' exhausted retries after {} attempts; payload={}",
                event.getClass().getSimpleName(), IntegrationEventRetryPolicy.MAX_ATTEMPTS, event, ex);
    }
}
