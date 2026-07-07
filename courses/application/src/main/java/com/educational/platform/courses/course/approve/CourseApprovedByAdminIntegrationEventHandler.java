package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
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
 * Event listener for {@link CourseApprovedByAdminIntegrationEvent}.
 */
@Component
public class CourseApprovedByAdminIntegrationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseApprovedByAdminIntegrationEventHandler.class);

    private final ApproveCourseCommandHandler approveCourseCommandHandler;

    public CourseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler approveCourseCommandHandler) {
        this.approveCourseCommandHandler = approveCourseCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2))
    public void handleCourseApprovedByAdminEvent(CourseApprovedByAdminIntegrationEvent event) {
        approveCourseCommandHandler.handle(new ApproveCourseCommand(event.courseId()));
    }

    @Recover
    public void recover(Exception e, CourseApprovedByAdminIntegrationEvent event) {
        LOGGER.error("Retries exhausted for CourseApprovedByAdminIntegrationEvent [courseId={}], event is lost",
                event.courseId(), e);
    }

}
