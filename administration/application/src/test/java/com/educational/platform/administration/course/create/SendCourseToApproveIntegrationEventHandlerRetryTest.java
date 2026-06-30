package com.educational.platform.administration.course.create;

import com.educational.platform.common.event.FailedEventStatus;
import com.educational.platform.common.event.FailedIntegrationEvent;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.event.IntegrationEventRetryHandler;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(SendCourseToApproveIntegrationEventHandlerRetryTest.TestConfig.class)
class SendCourseToApproveIntegrationEventHandlerRetryTest {

    @Autowired
    private SendCourseToApproveIntegrationEventHandler handler;

    @Autowired
    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @BeforeEach
    void resetMocks() {
        reset(createCourseProposalCommandHandler, failedEventRepository);
    }

    @Test
    void transientException_retriedThreeTimesThenDeadLettered() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        doThrow(new OptimisticLockingFailureException("x"))
                .when(createCourseProposalCommandHandler).handle(Mockito.any(CreateCourseProposalCommand.class));

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        verify(createCourseProposalCommandHandler, times(3)).handle(Mockito.any(CreateCourseProposalCommand.class));
        verify(failedEventRepository, times(1)).save(Mockito.any());
    }

    @Test
    void dataAccessException_retriedThreeTimesThenDeadLettered() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        doThrow(new DataIntegrityViolationException("db down"))
                .when(createCourseProposalCommandHandler).handle(Mockito.any(CreateCourseProposalCommand.class));

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        verify(createCourseProposalCommandHandler, times(3)).handle(Mockito.any(CreateCourseProposalCommand.class));
        verify(failedEventRepository, times(1)).save(Mockito.any());
    }

    @Test
    void deadLetteredRecord_capturesEventTypePayloadMessageAndMaxAttempts() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        doThrow(new OptimisticLockingFailureException("stale version"))
                .when(createCourseProposalCommandHandler).handle(Mockito.any(CreateCourseProposalCommand.class));

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        final ArgumentCaptor<FailedIntegrationEvent> captor = ArgumentCaptor.forClass(FailedIntegrationEvent.class);
        verify(failedEventRepository).save(captor.capture());
        final FailedIntegrationEvent deadLetter = captor.getValue();
        assertThat(deadLetter.getEventClassName()).isEqualTo(SendCourseToApproveIntegrationEvent.class.getName());
        assertThat(deadLetter.getEventPayload()).isEqualTo(String.valueOf(event));
        assertThat(deadLetter.getExceptionMessage()).isEqualTo("stale version");
        assertThat(deadLetter.getRetryCount()).isEqualTo(IntegrationEventRetryHandler.MAX_ATTEMPTS);
        assertThat(deadLetter.getStatus()).isEqualTo(FailedEventStatus.FAILED);
    }

    @Test
    void successPath_executesCommandOnceAndDoesNotDeadLetter() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        verify(createCourseProposalCommandHandler, times(1)).handle(Mockito.any(CreateCourseProposalCommand.class));
        verify(failedEventRepository, never()).save(Mockito.any());
    }

    @Test
    void businessException_notRetried() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        doThrow(new ResourceNotFoundException("x"))
                .when(createCourseProposalCommandHandler).handle(Mockito.any(CreateCourseProposalCommand.class));

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then: not retried (single attempt) but still dead-lettered, because the
        // @Recover method is declared on Throwable and therefore recovers any exception.
        verify(createCourseProposalCommandHandler, times(1)).handle(Mockito.any(CreateCourseProposalCommand.class));
        verify(failedEventRepository, times(1)).save(Mockito.any());
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        CreateCourseProposalCommandHandler createCourseProposalCommandHandler() {
            return Mockito.mock(CreateCourseProposalCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedEventRepository() {
            return Mockito.mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        SendCourseToApproveIntegrationEventHandler handler(CreateCourseProposalCommandHandler commandHandler,
                                                           FailedIntegrationEventRepository failedEventRepository) {
            return new SendCourseToApproveIntegrationEventHandler(commandHandler, failedEventRepository);
        }
    }
}
