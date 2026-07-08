package com.educational.platform.administration.course.approve;

import com.educational.platform.administration.course.CourseProposal;
import com.educational.platform.administration.course.CourseProposalRepository;
import com.educational.platform.administration.course.create.CreateCourseProposalCommand;
import com.educational.platform.administration.integration.event.CourseApprovedByAdminIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ApproveCourseProposalCommandHandlerTransactionTest.Config.class)
class ApproveCourseProposalCommandHandlerTransactionTest {

    @Autowired
    private ApproveCourseProposalCommandHandler sut;

    @Autowired
    private CourseProposalRepository repository;

    @Autowired
    private PublishProbe publishProbe;

    @Test
    void handle_publishesEventInsideActiveTransaction() {
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final ApproveCourseProposalCommand command = new ApproveCourseProposalCommand(uuid);

        final CreateCourseProposalCommand createCourseProposalCommand = new CreateCourseProposalCommand(uuid);
        final CourseProposal correspondingCourseProposal = new CourseProposal(createCourseProposalCommand);
        ReflectionTestUtils.setField(correspondingCourseProposal, "uuid", uuid);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(correspondingCourseProposal));

        sut.handle(command);

        verify(repository).save(correspondingCourseProposal);
        assertThat(publishProbe.transactionActiveDuringPublish.get()).isTrue();
        assertThat(publishProbe.event.get()).isEqualTo(new CourseApprovedByAdminIntegrationEvent(uuid));
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
        PublishProbe publishProbe() {
            return new PublishProbe();
        }

        @Bean
        ApproveCourseProposalCommandHandler approveCourseProposalCommandHandler(
                PlatformTransactionManager transactionManager,
                CourseProposalRepository repository,
                ApplicationEventPublisher eventPublisher) {
            return new ApproveCourseProposalCommandHandler(new TransactionTemplate(transactionManager), repository, eventPublisher);
        }
    }

    static class PublishProbe {

        private final AtomicBoolean transactionActiveDuringPublish = new AtomicBoolean();
        private final AtomicReference<CourseApprovedByAdminIntegrationEvent> event = new AtomicReference<>();

        @EventListener
        void handle(CourseApprovedByAdminIntegrationEvent event) {
            transactionActiveDuringPublish.set(TransactionSynchronizationManager.isActualTransactionActive());
            this.event.set(event);
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
