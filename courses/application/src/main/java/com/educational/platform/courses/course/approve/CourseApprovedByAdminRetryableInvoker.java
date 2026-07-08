package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class CourseApprovedByAdminRetryableInvoker {

    private static final Logger log = LoggerFactory.getLogger(CourseApprovedByAdminRetryableInvoker.class);

    private final ApproveCourseCommandHandler approveCourseCommandHandler;

    public CourseApprovedByAdminRetryableInvoker(ApproveCourseCommandHandler approveCourseCommandHandler) {
        this.approveCourseCommandHandler = approveCourseCommandHandler;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = IntegrationEventRetryPolicy.MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = IntegrationEventRetryPolicy.INITIAL_DELAY_MS,
                    multiplier = IntegrationEventRetryPolicy.MULTIPLIER,
                    maxDelay = IntegrationEventRetryPolicy.MAX_DELAY_MS))
    public void invoke(CourseApprovedByAdminIntegrationEvent event) {
        approveCourseCommandHandler.handle(new ApproveCourseCommand(event.courseId()));
    }

    @Recover
    public void recover(Exception ex, CourseApprovedByAdminIntegrationEvent event) {
        log.error("Integration event '{}' exhausted retries after {} attempts; payload={}",
                event.getClass().getSimpleName(), IntegrationEventRetryPolicy.MAX_ATTEMPTS, event, ex);
    }
}
