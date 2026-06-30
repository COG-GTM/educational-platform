package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link CourseApprovedByAdminIntegrationEvent}.
 */
@Component
public class CourseApprovedByAdminIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseApprovedByAdminIntegrationEventHandler.class);

    private final ApproveCourseCommandHandler approveCourseCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public CourseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler approveCourseCommandHandler,
                                                        FailedIntegrationEventRepository failedEventRepository) {
        this.approveCourseCommandHandler = approveCourseCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @EventListener
    @Retryable(retryFor = { DataAccessException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleCourseApprovedByAdminEvent(CourseApprovedByAdminIntegrationEvent event) {
        log.info("Received CourseApprovedByAdminIntegrationEvent: {}", event);
        try {
            approveCourseCommandHandler.handle(new ApproveCourseCommand(event.courseId()));
        } catch (Exception ex) {
            log.error("Failed to handle CourseApprovedByAdminIntegrationEvent: {}", event, ex);
            throw ex;
        }
    }

    @Recover
    public void recover(Throwable ex, CourseApprovedByAdminIntegrationEvent event) {
        log.error("Retries exhausted for CourseApprovedByAdminIntegrationEvent: {}, sending to dead-letter store", event, ex);
        final String payload = String.valueOf(event);
        failedEventRepository.save(FailedIntegrationEvent.of(event.getClass().getName(), payload, ex.getMessage(),
                IntegrationEventRetryHandler.MAX_ATTEMPTS));
    }

}
