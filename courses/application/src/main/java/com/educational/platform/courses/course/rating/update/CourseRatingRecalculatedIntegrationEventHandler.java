package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link CourseRatingRecalculatedIntegrationEvent}.
 */
@Component
public class CourseRatingRecalculatedIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseRatingRecalculatedIntegrationEventHandler.class);
    private static final int MAX_ATTEMPTS = 3;

    private final UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;
    private final FailedIntegrationEventRepository failedIntegrationEventRepository;

    public CourseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler,
                                                           FailedIntegrationEventRepository failedIntegrationEventRepository) {
        this.updateCourseRatingCommandHandler = updateCourseRatingCommandHandler;
        this.failedIntegrationEventRepository = failedIntegrationEventRepository;
    }

    @Async
    @Retryable(retryFor = {DataAccessException.class, ObjectOptimisticLockingFailureException.class},
               maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = 500, multiplier = 2))
    @EventListener
    public void handleCourseRatingRecalculatedEvent(CourseRatingRecalculatedIntegrationEvent event) {
        log.info("Received integration event: {}", event);
        try {
            updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(event.courseId(), event.rating()));
        } catch (Exception e) {
            log.error("Failed to handle integration event: {}", event, e);
            throw e;
        }
    }

    @Recover
    public void recover(DataAccessException e, CourseRatingRecalculatedIntegrationEvent event) {
        log.error("All retries exhausted for event: {}. Error: {}", event, e.getMessage(), e);
        failedIntegrationEventRepository.save(new FailedIntegrationEventRecord(
                event.getClass().getName(),
                event.toString(),
                e.getMessage(),
                e.getClass().getName(),
                MAX_ATTEMPTS
        ));
    }
}
