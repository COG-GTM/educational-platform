package com.educational.platform.courses.teacher.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.educational.platform.common.retry.IntegrationEventRetryPolicy;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringJUnitConfig(UserCreatedRetryableInvokerTest.TestConfig.class)
class UserCreatedRetryableInvokerTest {

    @MockitoBean
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Autowired
    private UserCreatedRetryableInvoker sut;

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(UserCreatedRetryableInvoker.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void invoke_success_commandBuiltFromEventUsernameAndSingleAttempt() {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher@example.com", "teacher@example.com");

        sut.invoke(event);

        final ArgumentCaptor<CreateTeacherCommand> argument = ArgumentCaptor.forClass(CreateTeacherCommand.class);
        verify(createTeacherCommandHandler, times(1)).handle(argument.capture());
        assertThat(argument.getValue()).hasFieldOrPropertyWithValue("username", event.username());
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void invoke_transientFailureThenSuccess_retriesAndReturns() {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher@example.com", "teacher@example.com");
        final AtomicInteger attempts = new AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            if (attempts.getAndIncrement() < IntegrationEventRetryPolicy.MAX_ATTEMPTS - 1) {
                throw new RuntimeException("transient");
            }
            return null;
        }).when(createTeacherCommandHandler).handle(any());

        assertThatCode(() -> sut.invoke(event)).doesNotThrowAnyException();
        verify(createTeacherCommandHandler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle(any());
        assertThat(listAppender.list).isEmpty();
    }

    @Test
    void invoke_exhaustion_logsErrorAndReturns() {
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("teacher@example.com", "teacher@example.com");
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(createTeacherCommandHandler).handle(any());

        assertThatCode(() -> sut.invoke(event)).doesNotThrowAnyException();
        verify(createTeacherCommandHandler, times(IntegrationEventRetryPolicy.MAX_ATTEMPTS)).handle(any());

        assertThat(listAppender.list).hasSize(1);
        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel()).isEqualTo(Level.ERROR);
        assertThat(loggingEvent.getFormattedMessage()).contains("Integration event 'UserCreatedIntegrationEvent' exhausted retries");
        assertThat(loggingEvent.getFormattedMessage()).contains(event.toString());
        assertThat(loggingEvent.getThrowableProxy().getMessage()).isEqualTo("boom");
    }

    @Configuration
    @EnableRetry
    @Import(UserCreatedRetryableInvoker.class)
    static class TestConfig {
    }
}
