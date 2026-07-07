package com.educational.platform.administration.course.create;

import com.educational.platform.courses.integration.event.SendCourseToApproveIntegrationEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class SendCourseToApproveIntegrationEventHandlerAfterCommitTest {

    private CreateCourseProposalCommandHandler createCourseProposalCommandHandler;

    private AnnotationConfigApplicationContext context;

    @Configuration
    @EnableTransactionManagement
    static class AfterCommitTestConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        SendCourseToApproveIntegrationEventHandler sendCourseToApproveIntegrationEventHandler(CreateCourseProposalCommandHandler handler) {
            return new SendCourseToApproveIntegrationEventHandler(handler);
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

    @BeforeEach
    void setUp() {
        createCourseProposalCommandHandler = mock(CreateCourseProposalCommandHandler.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(CreateCourseProposalCommandHandler.class, () -> createCourseProposalCommandHandler);
        context.register(AfterCommitTestConfiguration.class);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void publishEventInsideTransaction_handlerInvokedOnlyAfterCommit() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final TransactionTemplate transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        // when
        transactionTemplate.executeWithoutResult(status -> {
            context.publishEvent(new SendCourseToApproveIntegrationEvent(uuid));
            verifyNoInteractions(createCourseProposalCommandHandler);
        });

        // then
        verify(createCourseProposalCommandHandler).handle(any(CreateCourseProposalCommand.class));
    }

    @Test
    void publishEventInsideRolledBackTransaction_handlerNotInvoked() {
        // given
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final TransactionTemplate transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));

        // when
        transactionTemplate.executeWithoutResult(status -> {
            context.publishEvent(new SendCourseToApproveIntegrationEvent(uuid));
            status.setRollbackOnly();
        });

        // then
        verifyNoInteractions(createCourseProposalCommandHandler);
    }

}
