package com.educational.platform.administration.course.create;

import com.educational.platform.common.event.FailedIntegrationEventEntity;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.FailedIntegrationEventStatus;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;
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

import java.util.UUID;

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
 * {@link SendCourseToApproveIntegrationEventHandler} inside a minimal {@code @EnableRetry} context.
 */
@SpringJUnitConfig(SendCourseToApproveIntegrationEventHandlerRetryTest.TestConfig.class)
class SendCourseToApproveIntegrationEventHandlerRetryTest {

    private static final UUID COURSE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private SendCourseToApproveIntegrationEventHandler sut;

    @Autowired
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedIntegrationEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(createCourseProposalCommandHandler, failedIntegrationEventRepository);
    }

    @Test
    void retriesUpToMaxAttempts_onTransientException() {
        // given
        doThrow(new OptimisticLockingFailureException("transient"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));

        // then
        verify(createCourseProposalCommandHandler, times(3)).handle(any());
    }

    @Test
    void doesNotRetry_onBusinessException() {
        // given
        doThrow(new ResourceNotFoundException("not found"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));

        // then
        verify(createCourseProposalCommandHandler, times(1)).handle(any());
    }

    @Test
    void persistsFailedIntegrationEventEntity_afterRetriesExhausted() {
        // given
        doThrow(new OptimisticLockingFailureException("transient"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));

        // then
        final ArgumentCaptor<FailedIntegrationEventEntity> captor = ArgumentCaptor.forClass(FailedIntegrationEventEntity.class);
        verify(failedIntegrationEventRepository).save(captor.capture());
        final FailedIntegrationEventEntity saved = captor.getValue();
        assertThat(saved.getEventClassName()).isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
        assertThat(saved.getStatus()).isEqualTo(FailedIntegrationEventStatus.FAILED);
        assertThat(saved.getRetryCount()).isEqualTo(3);
        assertThat(saved.getEventPayload()).contains(COURSE_ID.toString());
    }

    @Test
    void persistsFailedIntegrationEventEntity_evenForNonRetryableBusinessException() {
        // given
        doThrow(new ResourceNotFoundException("not found"))
                .when(createCourseProposalCommandHandler).handle(any());

        // when
        sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));

        // then
        verify(failedIntegrationEventRepository).save(any(FailedIntegrationEventEntity.class));
    }

    @Test
    void doesNotPersist_onSuccess() {
        // when
        sut.handleSendCourseToApproveEvent(new SendCourseToApproveIntegrationEvent(COURSE_ID));

        // then
        verify(createCourseProposalCommandHandler, times(1)).handle(any());
        verify(failedIntegrationEventRepository, never()).save(any());
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        CreateCourseProposalCommandHandler createCourseProposalCommandHandler() {
            return mock(CreateCourseProposalCommandHandler.class);
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
        SendCourseToApproveIntegrationEventHandler sendCourseToApproveIntegrationEventHandler(
                CreateCourseProposalCommandHandler createCourseProposalCommandHandler,
                FailedIntegrationEventRepository failedIntegrationEventRepository,
                ObjectMapper objectMapper) {
            return new SendCourseToApproveIntegrationEventHandler(
                    createCourseProposalCommandHandler, failedIntegrationEventRepository, objectMapper);
        }
    }
}
