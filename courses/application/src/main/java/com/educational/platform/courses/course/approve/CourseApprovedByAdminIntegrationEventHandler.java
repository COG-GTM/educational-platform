package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
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
    @Retryable(retryFor = {TransientDataAccessException.class, OptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @EventListener
    public void handleCourseApprovedByAdminEvent(CourseApprovedByAdminIntegrationEvent event) {
        log.info("Received integration event: {}", event);
        try {
            approveCourseCommandHandler.handle(new ApproveCourseCommand(event.courseId()));
        } catch (Exception e) {
            log.error("Failed to handle integration event: {}", event, e);
            throw e;
        }
    }

    @Recover
    public void recover(DataAccessException e, CourseApprovedByAdminIntegrationEvent event) {
        log.error("All retries exhausted for event: {}. Error: {}", event, e.getMessage(), e);
        failedEventRepository.save(new FailedIntegrationEventRecord(
                event.getClass().getName(),
                event.toString(),
                e.getMessage() != null ? e.getMessage() : e.getClass().getName(),
                3
        ));
    }

}
