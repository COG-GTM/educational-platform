package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;

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
 * Event listener for {@link CourseRatingRecalculatedIntegrationEvent}.
 *
 * <p>Transient infrastructure failures are retried with exponential backoff; once retries
 * are exhausted the event is dead-lettered via {@link FailedIntegrationEventRepository}.
 * Business exceptions are not retried because retrying them cannot succeed.
 */
@Component
public class CourseRatingRecalculatedIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseRatingRecalculatedIntegrationEventHandler.class);

    private final UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public CourseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler,
                                                           FailedIntegrationEventRepository failedEventRepository) {
        this.updateCourseRatingCommandHandler = updateCourseRatingCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @EventListener
    @Retryable(retryFor = { DataAccessException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleCourseRatingRecalculatedEvent(CourseRatingRecalculatedIntegrationEvent event) {
        log.info("Received {}: {}", CourseRatingRecalculatedIntegrationEvent.class.getSimpleName(), event);
        try {
            updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(event.courseId(), event.rating()));
        } catch (Exception ex) {
            log.error("Failed to handle {}: {}", CourseRatingRecalculatedIntegrationEvent.class.getSimpleName(), event, ex);
            throw ex;
        }
    }

    @Recover
    public void recover(Throwable ex, CourseRatingRecalculatedIntegrationEvent event) {
        log.error("Retries exhausted for {}, dead-lettering event: {}",
                CourseRatingRecalculatedIntegrationEvent.class.getSimpleName(), event, ex);
        failedEventRepository.save(FailedIntegrationEvent.of(
                event.getClass().getName(),
                String.valueOf(event),
                ex.getMessage(),
                IntegrationEventRetryHandler.MAX_ATTEMPTS));
    }

}
