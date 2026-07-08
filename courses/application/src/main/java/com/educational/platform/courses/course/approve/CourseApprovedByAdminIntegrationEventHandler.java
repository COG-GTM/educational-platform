package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link CourseApprovedByAdminIntegrationEvent}.
 */
@Component
public class CourseApprovedByAdminIntegrationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CourseApprovedByAdminIntegrationEventHandler.class);

    private final ApproveCourseCommandHandler approveCourseCommandHandler;

    public CourseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler approveCourseCommandHandler) {
        this.approveCourseCommandHandler = approveCourseCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleCourseApprovedByAdminEvent(CourseApprovedByAdminIntegrationEvent event) {
        approveCourseCommandHandler.handle(new ApproveCourseCommand(event.courseId()));
    }

    @Recover
    public void recover(Throwable e, CourseApprovedByAdminIntegrationEvent event) {
        logger.error("Integration event ultimately failed after retries: {}", event, e);
    }

}
