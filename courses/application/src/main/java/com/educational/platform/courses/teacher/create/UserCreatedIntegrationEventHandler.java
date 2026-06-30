package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

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
 * Event listener for {@link UserCreatedIntegrationEvent}.
 */
@Component
public class UserCreatedIntegrationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);

    private final CreateTeacherCommandHandler createTeacherCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public UserCreatedIntegrationEventHandler(CreateTeacherCommandHandler createTeacherCommandHandler,
                                              FailedIntegrationEventRepository failedEventRepository) {
        this.createTeacherCommandHandler = createTeacherCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @EventListener
    @Retryable(retryFor = { DataAccessException.class, OptimisticLockingFailureException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        logger.info("Received UserCreatedIntegrationEvent for username '{}'", event.username());
        try {
            createTeacherCommandHandler.handle(new CreateTeacherCommand(event.username()));
        } catch (Exception ex) {
            logger.error("Failed to handle UserCreatedIntegrationEvent {}", event, ex);
            throw ex;
        }
    }

    @Recover
    public void recover(Throwable ex, UserCreatedIntegrationEvent event) {
        logger.error("Exhausted retries handling UserCreatedIntegrationEvent {}; persisting to dead-letter store", event, ex);
        failedEventRepository.save(FailedIntegrationEvent.of(
                event.getClass().getName(),
                String.valueOf(event),
                ex.getMessage(),
                IntegrationEventRetryHandler.MAX_ATTEMPTS));
    }

}
