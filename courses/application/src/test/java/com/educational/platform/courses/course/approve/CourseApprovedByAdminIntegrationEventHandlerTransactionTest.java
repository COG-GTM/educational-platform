package com.educational.platform.courses.course.approve;

import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(CourseApprovedByAdminIntegrationEventHandlerTransactionTest.TestConfiguration.class)
class CourseApprovedByAdminIntegrationEventHandlerTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ApproveCourseCommandHandler approveCourseCommandHandler;

    @Test
    void handleCourseApprovedByAdminEvent_publishedInsideCommittedTransaction_handledAfterCommit() {
        // given
        reset(approveCourseCommandHandler);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));
            verify(approveCourseCommandHandler, never()).handle(any(ApproveCourseCommand.class));
        });

        // then
        verify(approveCourseCommandHandler).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void handleCourseApprovedByAdminEvent_publishedInsideRolledBackTransaction_notHandled() {
        // given
        reset(approveCourseCommandHandler);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));
                    throw new IllegalStateException("rollback");
                }));

        // then
        verify(approveCourseCommandHandler, never()).handle(any(ApproveCourseCommand.class));
    }

    @Test
    void handleCourseApprovedByAdminEvent_publishedWithoutTransaction_notHandled() {
        // given
        reset(approveCourseCommandHandler);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");

        // when
        eventPublisher.publishEvent(new CourseApprovedByAdminIntegrationEvent(uuid));

        // then
        verify(approveCourseCommandHandler, never()).handle(any(ApproveCourseCommand.class));
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

        @Bean
        ApproveCourseCommandHandler approveCourseCommandHandler() {
            return mock(ApproveCourseCommandHandler.class);
        }

        @Bean
        CourseApprovedByAdminIntegrationEventHandler courseApprovedByAdminIntegrationEventHandler(ApproveCourseCommandHandler handler) {
            return new CourseApprovedByAdminIntegrationEventHandler(handler);
        }
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() throws TransactionException {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        }
    }
}
