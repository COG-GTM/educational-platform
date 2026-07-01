package com.educational.platform.courses.teacher.create;

import com.educational.platform.common.event.FailedIntegrationEventEntity;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.FailedIntegrationEventStatus;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.users.integration.event.UserCreatedIntegrationEvent;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Exercises the Spring-AOP proxied {@link org.springframework.retry.annotation.Retryable} /
 * {@link org.springframework.retry.annotation.Recover} behaviour of
 * {@link UserCreatedIntegrationEventHandler} inside a minimal {@code @EnableRetry} context.
 */
@SpringJUnitConfig(UserCreatedIntegrationEventHandlerRetryTest.TestConfig.class)
class UserCreatedIntegrationEventHandlerRetryTest {

    @Autowired
    private UserCreatedIntegrationEventHandler sut;

    @Autowired
    private CreateTeacherCommandHandler createTeacherCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(createTeacherCommandHandler, failedIntegrationEventRepository);
    }

    private UserCreatedIntegrationEvent event() {
        return new UserCreatedIntegrationEvent("username", "user@example.com");
    }

    @Test
    void retriesUpToMaxAttempts_onTransientException() {
        // given
        doThrow(new OptimisticLockingFailureException("transient"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        sut.handleUserCreatedEvent(event());

        // then
        verify(createTeacherCommandHandler, times(3)).handle(any());
    }

    @Test
    void doesNotRetry_onBusinessException() {
        // given
        doThrow(new ResourceNotFoundException("not found"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        sut.handleUserCreatedEvent(event());

        // then
        verify(createTeacherCommandHandler, times(1)).handle(any());
    }

    @Test
    void persistsFailedIntegrationEventEntity_evenForNonRetryableBusinessException() {
        // given
        doThrow(new ResourceNotFoundException("not found"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        sut.handleUserCreatedEvent(event());

        // then
        verify(failedIntegrationEventRepository).save(any(FailedIntegrationEventEntity.class));
    }

    @Test
    void persistsFailedIntegrationEventEntity_afterRetriesExhausted() {
        // given
        doThrow(new OptimisticLockingFailureException("transient"))
                .when(createTeacherCommandHandler).handle(any());

        // when
        sut.handleUserCreatedEvent(event());

        // then
        final ArgumentCaptor<FailedIntegrationEventEntity> captor = ArgumentCaptor.forClass(FailedIntegrationEventEntity.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final FailedIntegrationEventEntity saved = captor.getValue();
        assertThat(saved.getEventClassName()).isEqualTo(UserCreatedIntegrationEvent.class.getName());
        assertThat(saved.getStatus()).isEqualTo(FailedIntegrationEventStatus.FAILED);
        assertThat(saved.getRetryCount()).isEqualTo(3);
        assertThat(saved.getEventPayload()).contains("username");
    }

    @Test
    void doesNotPersist_onSuccess() {
        // when
        sut.handleUserCreatedEvent(event());

        // then
        verify(createTeacherCommandHandler, times(1)).handle(any());
        verify(failedIntegrationEventRepository, never()).save(any());
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        CreateTeacherCommandHandler createTeacherCommandHandler() {
            return mock(CreateTeacherCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        UserCreatedIntegrationEventHandler userCreatedIntegrationEventHandler(
                CreateTeacherCommandHandler createTeacherCommandHandler,
                FailedIntegrationEventRepository failedIntegrationEventRepository,
                ObjectMapper objectMapper) {
            return new UserCreatedIntegrationEventHandler(
                    createTeacherCommandHandler, failedIntegrationEventRepository, objectMapper);
        }
    }
}
