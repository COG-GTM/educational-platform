package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for {@link UserCreatedIntegrationEvent}.
 */
@Component
public class UserCreatedIntegrationEventHandler {

    private final UserCreatedRetryableInvoker userCreatedRetryableInvoker;

    public UserCreatedIntegrationEventHandler(UserCreatedRetryableInvoker userCreatedRetryableInvoker) {
        this.userCreatedRetryableInvoker = userCreatedRetryableInvoker;
    }

    @Async("integrationEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreatedEvent(UserCreatedIntegrationEvent event) {
        userCreatedRetryableInvoker.invoke(event);
    }

}
