package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Exercises the declarative {@code @Retryable}/{@code @Recover} behaviour of
 * {@link UserCreatedIntegrationEventHandler} through a real Spring context, since plain
 * Mockito does not honour those annotations.
 */
@SpringJUnitConfig(UserCreatedIntegrationEventHandlerRetryTest.TestConfig.class)
class UserCreatedIntegrationEventHandlerRetryTest {

    @Autowired
    private UserCreatedIntegrationEventHandler handler;

    @Autowired
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(createTeacherCommandHandler, failedEventRepository);
    }

    @Test
    void transientException_retriedAndRecovered() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");
        doThrow(new OptimisticLockingFailureException("boom"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        handler.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler, times(3)).handle(any(CreateTeacherCommand.class));
        verify(failedEventRepository, times(1)).save(any());
    }

    @Test
    void dataAccessException_retriedAndRecovered() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");
        doThrow(new RecoverableDataAccessException("db down"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        handler.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler, times(3)).handle(any(CreateTeacherCommand.class));
        verify(failedEventRepository, times(1)).save(any());
    }

    @Test
    void businessException_notRetried() {
        // given
        final UserCreatedIntegrationEvent event = new UserCreatedIntegrationEvent("username", "user@example.com");
        doThrow(new ResourceNotFoundException("nope"))
                .when(createTeacherCommandHandler).handle(any(CreateTeacherCommand.class));

        // when
        handler.handleUserCreatedEvent(event);

        // then
        verify(createTeacherCommandHandler, times(1)).handle(any(CreateTeacherCommand.class));
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        UserCreatedIntegrationEventHandler handler(CreateTeacherCommandHandler createTeacherCommandHandler,
                                                   FailedIntegrationEventRepository failedEventRepository) {
            return new UserCreatedIntegrationEventHandler(createTeacherCommandHandler, failedEventRepository);
        }

        @Bean
        CreateTeacherCommandHandler createTeacherCommandHandler() {
            return mock(CreateTeacherCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }
    }
}
