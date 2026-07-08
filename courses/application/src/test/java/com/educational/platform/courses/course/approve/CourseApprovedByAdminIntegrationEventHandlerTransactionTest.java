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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the {@code @TransactionalEventListener(phase = AFTER_COMMIT)} contract of
 * {@link CourseApprovedByAdminIntegrationEventHandler}: the handler consumes events only
 * after the publishing transaction commits, and drops them on rollback or when published
 * without a transaction.
 */
@SpringJUnitConfig(CourseApprovedByAdminIntegrationEventHandlerTransactionTest.Config.class)
class CourseApprovedByAdminIntegrationEventHandlerTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @BeforeEach
    void resetMocks() {
        reset(approveCourseCommandHandler);
    }

    @Test
    void publishedInsideTransaction_committed_handlerExecutedAfterCommit() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));
            // then: not yet delivered inside the transaction
            verifyNoInteractions(approveCourseCommandHandler);
        });

        // then: delivered after commit
        verify(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void publishedInsideTransaction_rolledBack_handlerNotExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));
            status.setRollbackOnly();
        });

        // then
        verifyNoInteractions(approveCourseCommandHandler);
    }

    @Test
    void publishedWithoutTransaction_handlerNotExecuted() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));

        // then: fallbackExecution defaults to false, so the event is dropped
        verifyNoInteractions(approveCourseCommandHandler);
    }

    @Configuration
    static class Config {

        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler courseApprovedByAdminIntegrationEventHandler(
                ApproveCourseCommandHandler approveCourseCommandHandler) {
            return new CourseApprovedByAdminIntegrationEventHandler(approveCourseCommandHandler);
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
