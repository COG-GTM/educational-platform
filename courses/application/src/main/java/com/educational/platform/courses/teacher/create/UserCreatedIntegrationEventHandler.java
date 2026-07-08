package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

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
 * Event listener for {@link UserCreatedIntegrationEvent}. Executed asynchronously
 * after the publishing transaction commits, with retry on failure.
 */
@Component
public class UserCreatedIntegrationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);

    private final CreateTeacherCommandHandler createTeacherCommandHandler;

    public UserCreatedIntegrationEventHandler(CreateTeacherCommandHandler createTeacherCommandHandler) {
        this.createTeacherCommandHandler = createTeacherCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        createTeacherCommandHandler.handle(new CreateTeacherCommand(event.username()));
    }

    @Recover
    void recover(Exception exception, UserCreatedIntegrationEvent event) {
        log.error("Failed to process {} after retries, event will be dropped: {}",
                event.getClass().getSimpleName(), event, exception);
    }

}
