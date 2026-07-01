package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventEntity;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
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
 * Event listener for {@link UserCreatedIntegrationEvent}.
 */
// todo should be transactional?
@Component
public class UserCreatedIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);

    private final CreateTeacherCommandHandler createTeacherCommandHandler;
    private final FailedIntegrationEventRepository failedIntegrationEventRepository;
    private final ObjectMapper objectMapper;

    public UserCreatedIntegrationEventHandler(CreateTeacherCommandHandler createTeacherCommandHandler,
                                              FailedIntegrationEventRepository failedIntegrationEventRepository,
                                              ObjectMapper objectMapper) {
        this.createTeacherCommandHandler = createTeacherCommandHandler;
        this.failedIntegrationEventRepository = failedIntegrationEventRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @EventListener
    @Retryable(
            retryFor = {TransientDataAccessException.class, DataAccessResourceFailureException.class, OptimisticLockingFailureException.class},
            maxAttempts = IntegrationEventRetryHandler.MAX_ATTEMPTS,
            backoff = @Backoff(delay = IntegrationEventRetryHandler.INITIAL_BACKOFF_MS, multiplier = IntegrationEventRetryHandler.BACKOFF_MULTIPLIER))
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        log.info("Received {}: {}", event.getClass().getSimpleName(), event);
        try {
            createTeacherCommandHandler.handle(new CreateTeacherCommand(event.username()));
        } catch (Exception e) {
            log.error("Failed to handle {}: {}", event.getClass().getSimpleName(), event, e);
            throw e;
        }
    }

    @Recover
    public void recover(Exception e, UserCreatedIntegrationEvent event) {
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
