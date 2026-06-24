package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
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

    private static final Logger log = LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);

    private final CreateTeacherCommandHandler createTeacherCommandHandler;
    private final FailedIntegrationEventRepository failedEventRepository;

    public UserCreatedIntegrationEventHandler(CreateTeacherCommandHandler createTeacherCommandHandler,
                                               FailedIntegrationEventRepository failedEventRepository) {
        this.createTeacherCommandHandler = createTeacherCommandHandler;
        this.failedEventRepository = failedEventRepository;
    }

    @Async
    @Retryable(retryFor = {TransientDataAccessException.class, OptimisticLockingFailureException.class, PessimisticLockingFailureException.class},
               maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    @EventListener
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        log.info("Received integration event: {}", event);
        try {
            createTeacherCommandHandler.handle(new CreateTeacherCommand(event.username()));
        } catch (Exception e) {
            log.error("Failed to handle integration event: {}", event, e);
            throw e;
        }
    }

    @Recover
    public void recover(Exception e, UserCreatedIntegrationEvent event) {
        log.error("All retries exhausted for event: {}. Error: {}", event, e.getMessage(), e);
        failedEventRepository.save(new FailedIntegrationEventRecord(
                event.getClass().getName(),
                event.toString(),
                e.getMessage(),
                3
        ));
    }

}
