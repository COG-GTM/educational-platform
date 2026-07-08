package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class UserCreatedRetryableInvoker {

    private static final Logger log = LoggerFactory.getLogger(UserCreatedRetryableInvoker.class);

    private final CreateTeacherCommandHandler createTeacherCommandHandler;

    public UserCreatedRetryableInvoker(CreateTeacherCommandHandler createTeacherCommandHandler) {
        this.createTeacherCommandHandler = createTeacherCommandHandler;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = IntegrationEventRetryPolicy.MAX_ATTEMPTS,
            backoff = @Backoff(
                    delay = IntegrationEventRetryPolicy.INITIAL_DELAY_MS,
                    multiplier = IntegrationEventRetryPolicy.MULTIPLIER,
                    maxDelay = IntegrationEventRetryPolicy.MAX_DELAY_MS))
    public void invoke(UserCreatedIntegrationEvent event) {
        createTeacherCommandHandler.handle(new CreateTeacherCommand(event.username()));
    }

    @Recover
    public void recover(Exception ex, UserCreatedIntegrationEvent event) {
        log.error("Integration event '{}' exhausted retries after {} attempts; payload={}",
                event.getClass().getSimpleName(), IntegrationEventRetryPolicy.MAX_ATTEMPTS, event, ex);
    }
}
