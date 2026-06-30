package com.educational.platform.administration.course.create;

import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.common.exception.ResourceNotFoundException;
import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
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
    void businessException_notRetried() {
        // given
        final SendCourseToApproveIntegrationEvent event = new SendCourseToApproveIntegrationEvent(UUID.randomUUID());
        doThrow(new ResourceNotFoundException("x"))
                .when(createCourseProposalCommandHandler).handle(Mockito.any(CreateCourseProposalCommand.class));

        // when
        handler.handleSendCourseToApproveEvent(event);

        // then
        verify(createCourseProposalCommandHandler, times(1)).handle(Mockito.any(CreateCourseProposalCommand.class));
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
