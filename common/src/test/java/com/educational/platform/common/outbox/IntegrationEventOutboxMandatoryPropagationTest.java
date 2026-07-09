package com.educational.platform.common.outbox;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class IntegrationEventOutboxMandatoryPropagationTest {

    private AnnotationConfigApplicationContext context;
    private IntegrationEventOutboxRepository repository;
    private IntegrationEventOutbox sut;
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        repository = mock(IntegrationEventOutboxRepository.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(IntegrationEventOutboxRepository.class, () -> repository);
        context.register(TestConfiguration.class);
        context.refresh();
        sut = context.getBean(IntegrationEventOutbox.class);
        transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void publish_withoutActiveTransaction_illegalTransactionStateException() {
        // given
        final SampleIntegrationEvent event = new SampleIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));

        // when / then
        assertThatExceptionOfType(IllegalTransactionStateException.class)
                .isThrownBy(() -> sut.publish(event));
        verify(repository, never()).save(any());
    }

    @Test
    void publish_withinActiveTransaction_entrySaved() {
        // given
        final SampleIntegrationEvent event = new SampleIntegrationEvent(UUID.fromString("123e4567-e89b-12d3-a456-426655440001"));

        // when
        transactionTemplate.executeWithoutResult(status -> sut.publish(event));

        // then
        verify(repository).save(any(IntegrationEventOutboxEntry.class));
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        IntegrationEventOutbox integrationEventOutbox(IntegrationEventOutboxRepository repository) {
            return new IntegrationEventOutbox(repository);
        }
    }

    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return TransactionSynchronizationManager.isActualTransactionActive();
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
