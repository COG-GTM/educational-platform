package com.educational.platform.courses.course.rating.update;

import com.educational.platform.common.event.FailedIntegrationEventEntity;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.course.reviews.integration.event.CourseRatingRecalculatedIntegrationEvent;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
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

    private final UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler;
    private final FailedIntegrationEventRepository failedIntegrationEventRepository;
    private final ObjectMapper objectMapper;

    public CourseRatingRecalculatedIntegrationEventHandler(UpdateCourseRatingCommandHandler updateCourseRatingCommandHandler,
                                                           FailedIntegrationEventRepository failedIntegrationEventRepository,
                                                           ObjectMapper objectMapper) {
        this.updateCourseRatingCommandHandler = updateCourseRatingCommandHandler;
        this.failedIntegrationEventRepository = failedIntegrationEventRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    @Retryable(
            retryFor = {TransientDataAccessException.class, DataAccessResourceFailureException.class, OptimisticLockingFailureException.class},
            maxAttempts = IntegrationEventRetryHandler.MAX_ATTEMPTS,
            backoff = @Backoff(delay = IntegrationEventRetryHandler.INITIAL_BACKOFF_MS, multiplier = IntegrationEventRetryHandler.BACKOFF_MULTIPLIER))
    public void handleCourseRatingRecalculatedEvent(CourseRatingRecalculatedIntegrationEvent event) {
        log.info("Received {}: {}", event.getClass().getSimpleName(), event);
        try {
            updateCourseRatingCommandHandler.handle(new UpdateCourseRatingCommand(event.courseId(), event.rating()));
        } catch (Exception e) {
            log.error("Failed to handle {}: {}", event.getClass().getSimpleName(), event, e);
            throw e;
        }
    }

    @Recover
    public void recover(Exception e, CourseRatingRecalculatedIntegrationEvent event) {
        log.error("Retries exhausted handling {}: {}. Persisting to dead-letter store.",
                event.getClass().getSimpleName(), event, e);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception serializationError) {
            payload = String.valueOf(event);
        }
        failedIntegrationEventRepository.save(new FailedIntegrationEventEntity(event.getClass().getName(), payload,
                e.getMessage(), IntegrationEventRetryHandler.MAX_ATTEMPTS));
    }

}
