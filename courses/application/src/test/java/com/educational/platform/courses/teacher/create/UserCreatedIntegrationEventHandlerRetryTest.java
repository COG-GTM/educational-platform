package com.educational.platform.courses.teacher.create;

import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig
class UserCreatedIntegrationEventHandlerRetryTest {

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        CreateTeacherCommandHandler createTeacherCommandHandler() {
            return Mockito.mock(CreateTeacherCommandHandler.class);
        }

        @Bean
        UserCreatedIntegrationEventHandler handler(CreateTeacherCommandHandler commandHandler) {
            return new UserCreatedIntegrationEventHandler(commandHandler);
        }
    }

    @Autowired
    private UserCreatedIntegrationEventHandler sut;

    @Autowired
    private CreateTeacherCommandHandler commandHandler;

    @BeforeEach
    void resetMock() {
        Mockito.reset(commandHandler);
    }

    @Test
    void handle_validEvent_createTeacherCommandExecuted() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(commandHandler).handle(argument.capture());
        assertThat(argument.getValue())
                .hasFieldOrPropertyWithValue("username", "username");
    }

    @Test
    void handle_commandHandlerKeepsFailing_retriesThreeTimesThenRecovers() {
        // given
        doThrow(new RuntimeException("boom")).when(commandHandler).handle(any(CreateTeacherCommand.class));
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");

        // when - @Recover swallows the exception after retries are exhausted
        sut.handleUserCreatedEvent(event);

        // then
        verify(commandHandler, times(3)).handle(any(CreateTeacherCommand.class));
    }

    @Test
    void handle_transientFailure_succeedsOnSecondAttempt() {
        // given
        doThrow(new RuntimeException("transient"))
                .doNothing()
                .when(commandHandler).handle(any(CreateTeacherCommand.class));
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "email@gmail.com");

        // when
        sut.handleUserCreatedEvent(event);

        // then
        verify(commandHandler, times(2)).handle(any(CreateTeacherCommand.class));
    }

    @Test
    void listenerMethod_isAsyncAndAfterCommit() throws NoSuchMethodException {
        final Method method = UserCreatedIntegrationEventHandler.class
                .getMethod("handleUserCreatedEvent", UserCreatedIntegrationEvent.class);

        final Async async = method.getAnnotation(Async.class);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("integrationEventExecutor");

        final TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);
        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
