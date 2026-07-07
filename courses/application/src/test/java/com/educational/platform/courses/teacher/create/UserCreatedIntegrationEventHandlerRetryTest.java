package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UserCreatedIntegrationEventHandlerRetryTest {

    private CreateTeacherCommandHandler createTeacherCommandHandler;

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableResilientMethods
    static class RetryTestConfiguration {

        @Bean
        UserCreatedIntegrationEventHandler userCreatedIntegrationEventHandler(CreateTeacherCommandHandler handler) {
            return new UserCreatedIntegrationEventHandler(handler);
        }

    }

    @BeforeEach
    void setUp() {
        createTeacherCommandHandler = mock(CreateTeacherCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(CreateTeacherCommandHandler.class, () -> createTeacherCommandHandler);
        context.register(RetryTestConfiguration.class);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void handleUserCreatedEvent_transientFailure_retriedUntilSuccess() {
        // given
        doThrow(new RuntimeException("transient failure"))
                .doThrow(new RuntimeException("transient failure"))
                .doNothing()
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));
        final UserCreatedIntegrationEventHandler sut = context.getBean(UserCreatedIntegrationEventHandler.class);

        // when
        sut.handleUserCreatedEvent(new UserCreatedIntegrationEvent("teacher", "teacher@example.com"));

        // then
        verify(createTeacherCommandHandler, times(3)).handle(new CreateTeacherCommand("teacher"));
    }

    @Test
    void handleUserCreatedEvent_persistentFailure_retriesExhaustedAndExceptionPropagated() {
        // given
        doThrow(new RuntimeException("persistent failure"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));
        final UserCreatedIntegrationEventHandler sut = context.getBean(UserCreatedIntegrationEventHandler.class);

        // when // then
        assertThrows(RuntimeException.class,
                () -> sut.handleUserCreatedEvent(new UserCreatedIntegrationEvent("teacher", "teacher@example.com")));
        verify(createTeacherCommandHandler, times(4)).handle(any(CreateTeacherCommand.class));
    }

    @Test
    void publishEventWithoutTransaction_fallbackExecution_handlerInvoked() {
        // given
        doNothing().when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        context.publishEvent(new UserCreatedIntegrationEvent("teacher", "teacher@example.com"));

        // then
        verify(createTeacherCommandHandler).handle(new CreateTeacherCommand("teacher"));
    }

}
