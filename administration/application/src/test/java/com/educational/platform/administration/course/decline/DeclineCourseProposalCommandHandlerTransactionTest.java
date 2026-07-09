package com.educational.platform.administration.course.decline;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.integration.event.CourseDeclinedByAdminIntegrationEvent;
import com.educational.platform.common.outbox.IntegrationEventOutbox;
import com.educational.platform.common.outbox.IntegrationEventOutboxEntry;
import com.educational.platform.common.outbox.IntegrationEventOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(DeclineCourseProposalCommandHandlerTransactionTest.Config.class)
class DeclineCourseProposalCommandHandlerTransactionTest {

    @Autowired
    private DeclineCourseProposalCommandHandler sut;

    @Autowired
    private CourseProposalRepository repository;

    @Autowired
    private OutboxSaveProbe outboxSaveProbe;

    @Test
    void handle_storesEventInOutboxInsideActiveTransaction() throws Exception {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final DeclineCourseProposalCommand command = new DeclineCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        sut.handle(command);

        verify(repository).save(correspondingCourseProposal);
        assertThat(outboxSaveProbe.transactionActiveDuringSave.get()).isTrue();
        final IntegrationEventOutboxEntry entry = outboxSaveProbe.entry.get();
        assertThat(entry.getEventType()).isEqualTo(CourseDeclinedByAdminIntegrationEvent.class.getName());
        assertThat(new ObjectMapper().readValue(entry.getPayload(), CourseDeclinedByAdminIntegrationEvent.class))
                .isEqualTo(new CourseDeclinedByAdminIntegrationEvent(uuid));
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }

        @Bean
        CourseProposalRepository repository() {
            return mock(CourseProposalRepository.class);
        }

        @Bean
        OutboxSaveProbe outboxSaveProbe() {
            return new OutboxSaveProbe();
        }

        @Bean
        IntegrationEventOutboxRepository outboxRepository(OutboxSaveProbe outboxSaveProbe) {
            final IntegrationEventOutboxRepository outboxRepository = mock(IntegrationEventOutboxRepository.class);
            when(outboxRepository.save(any(IntegrationEventOutboxEntry.class))).thenAnswer(invocation -> {
                outboxSaveProbe.transactionActiveDuringSave.set(TransactionSynchronizationManager.isActualTransactionActive());
                outboxSaveProbe.entry.set(invocation.getArgument(0));
                return invocation.getArgument(0);
            });
            return outboxRepository;
        }

        @Bean
        IntegrationEventOutbox integrationEventOutbox(IntegrationEventOutboxRepository outboxRepository) {
            return new IntegrationEventOutbox(outboxRepository);
        }

        @Bean
        DeclineCourseProposalCommandHandler declineCourseProposalCommandHandler(
                PlatformTransactionManager transactionManager,
                CourseProposalRepository repository,
                IntegrationEventOutbox integrationEventOutbox) {
            return new DeclineCourseProposalCommandHandler(new TransactionTemplate(transactionManager), repository, integrationEventOutbox);
        }
    }

    static class OutboxSaveProbe {

        private final AtomicBoolean transactionActiveDuringSave = new AtomicBoolean();
        private final AtomicReference<IntegrationEventOutboxEntry> entry = new AtomicReference<>();
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
