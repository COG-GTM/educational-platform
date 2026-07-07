package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UserCreatedIntegrationEventHandlerRetryTest {

    @EnableRetry
    @Configuration
    static class RetryConfig {
    }

    @Test
    void handleUserCreatedEvent_commandHandlerAlwaysFails_retriedThreeTimesThenRecovered() {
        final CreateTeacherCommandHandler commandHandler = mock(CreateTeacherCommandHandler.class);
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any());
        final UserCreatedIntegrationEventHandler target = spy(new UserCreatedIntegrationEventHandler(commandHandler));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(RetryConfig.class);
            context.registerBean(UserCreatedIntegrationEventHandler.class, () -> target);
            context.refresh();

            final UserCreatedIntegrationEventHandler sut = context.getBean(UserCreatedIntegrationEventHandler.class);
            final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");

            sut.handleUserCreatedEvent(event);

            verify(commandHandler, times(3)).handle(any());
            verify(target).recover(any(RuntimeException.class), any(UserCreatedIntegrationEvent.class));
        }
    }

}
