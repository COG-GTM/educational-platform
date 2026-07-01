package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventEntity;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
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
 * Event listener for {@link CourseApprovedByAdminIntegrationEvent}.
 */
@Component
public class CourseApprovedByAdminIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseApprovedByAdminIntegrationEventHandler.class);

    private final ApproveCourseCommandHandler approveCourseCommandHandler;
    private final FailedIntegrationEventRepository failedIntegrationEventRepository;
    private final ObjectMapper objectMapper;

    public CourseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler approveCourseCommandHandler,
                                                        FailedIntegrationEventRepository failedIntegrationEventRepository,
                                                        ObjectMapper objectMapper) {
        this.approveCourseCommandHandler = approveCourseCommandHandler;
        this.failedIntegrationEventRepository = failedIntegrationEventRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    @Retryable(
            retryFor = {TransientDataAccessException.class, DataAccessResourceFailureException.class, OptimisticLockingFailureException.class},
            maxAttempts = IntegrationEventRetryHandler.MAX_ATTEMPTS,
            backoff = @Backoff(delay = IntegrationEventRetryHandler.INITIAL_BACKOFF_MS, multiplier = IntegrationEventRetryHandler.BACKOFF_MULTIPLIER))
    public void handleCourseApprovedByAdminEvent(CourseApprovedByAdminIntegrationEvent event) {
        log.info("Received {}: {}", event.getClass().getSimpleName(), event);
        try {
            approveCourseCommandHandler.handle(new ApproveCourseCommand(event.courseId()));
        } catch (Exception e) {
            log.error("Failed to handle {}: {}", event.getClass().getSimpleName(), event, e);
            throw e;
        }
    }

    @Recover
    public void recover(Exception e, CourseApprovedByAdminIntegrationEvent event) {
        log.error("Retries exhausted handling {}: {}. Persisting to dead-letter store.",
                event.getClass().getSimpleName(), event, e);
        failedIntegrationEventRepository.save(toFailedEvent(event, e, objectMapper));
    }

    private static FailedIntegrationEventEntity toFailedEvent(Object event, Exception e, ObjectMapper objectMapper) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception serializationError) {
            payload = String.valueOf(event);
        }
        return new FailedIntegrationEventEntity(event.getClass().getName(), payload, e.getMessage(),
                IntegrationEventRetryHandler.MAX_ATTEMPTS);
    }

}
