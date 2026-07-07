package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the transactional delivery semantics of the integration event listener:
 * events are delivered only after a successful commit, are dropped on rollback,
 * and are dropped when published without an active transaction (fallbackExecution=false).
 */
@SpringJUnitConfig(CourseApprovedByAdminIntegrationEventHandlerTransactionTest.Config.class)
public class CourseApprovedByAdminIntegrationEventHandlerTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @BeforeEach
    void resetMocks() {
        reset(approveCourseCommandHandler);
    }

    @Test
    void publishInsideTransaction_committed_handlerInvokedAfterCommit() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));

            // not delivered before the commit
            verifyNoInteractions(approveCourseCommandHandler);
        });

        // then
        verify(approveCourseCommandHandler).handle(new ApproveCourseCommand(uuid));
    }

    @Test
    void publishInsideTransaction_rolledBack_handlerNotInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));
            status.setRollbackOnly();
        });

        // then
        verify(approveCourseCommandHandler, never()).handle(any());
    }

    @Test
    void publishWithoutTransaction_handlerNotInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));

        // then
        verify(approveCourseCommandHandler, never()).handle(any());
    }

    @Configuration
    static class Config {

        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler courseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler approveCourseCommandHandler) {
            return new CourseApprovedByAdminIntegrationEventHandler(approveCourseCommandHandler);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

}
