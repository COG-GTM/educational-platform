package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for {@link UserCreatedIntegrationEvent}.
 */
@Component
public class UserCreatedIntegrationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(UserCreatedIntegrationEventHandler.class);

    private final CreateTeacherCommandHandler createTeacherCommandHandler;

    public UserCreatedIntegrationEventHandler(CreateTeacherCommandHandler createTeacherCommandHandler) {
        this.createTeacherCommandHandler = createTeacherCommandHandler;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        createTeacherCommandHandler.handle(new CreateTeacherCommand(event.username()));
    }

    @Recover
    public void recover(Throwable e, UserCreatedIntegrationEvent event) {
        logger.error("Integration event ultimately failed after retries: {}", event, e);
    }

}
