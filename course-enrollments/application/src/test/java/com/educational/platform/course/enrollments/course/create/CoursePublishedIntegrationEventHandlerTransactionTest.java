package com.educational.platform.course.enrollments.course.create;

import com.educational.platform.courses.integration.event.CoursePublishedIntegrationEvent;

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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pins the delivery semantics of the course replication: the snapshot is applied only once the publishing transaction
 * committed, never for a rolled back publication, and still when the event is published outside of a transaction.
 */
@SpringJUnitConfig(CoursePublishedIntegrationEventHandlerTransactionTest.TestConfiguration.class)
public class CoursePublishedIntegrationEventHandlerTransactionTest {

    private static final UUID COURSE = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
    private static final UUID LECTURE = UUID.fromString("223e4567-e89b-12d3-a456-426655440001");

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private CreateEnrollmentCourseCommandHandler createEnrollmentCourseCommandHandler;

    @BeforeEach
    void setUp() {
        reset(createEnrollmentCourseCommandHandler);
    }

    @Test
    void handleCoursePublishedEvent_publishedInsideTransaction_courseReplicatedOnlyAfterCommit() {
        // given
        final CoursePublishedIntegrationEvent event = event();

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);

            // then (still inside the transaction)
            verifyNoInteractions(createEnrollmentCourseCommandHandler);
        });

        // then (after commit)
        verify(createEnrollmentCourseCommandHandler).handle(new CreateCourseCommand(COURSE, "Java Basics", List.of(
                new CreateCourseCommand.CreateLectureCommand(LECTURE, "Intro", 1))));
    }

    @Test
    void handleCoursePublishedEvent_publishingTransactionRolledBack_courseNotReplicated() {
        // given
        final CoursePublishedIntegrationEvent event = event();

        // when
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        // then
        verifyNoInteractions(createEnrollmentCourseCommandHandler);
    }

    @Test
    void handleCoursePublishedEvent_publishedWithoutTransaction_courseReplicatedImmediately() {
        // given
        final CoursePublishedIntegrationEvent event = event();

        // when
        eventPublisher.publishEvent(event);

        // then
        verify(createEnrollmentCourseCommandHandler).handle(any(CreateCourseCommand.class));
    }

    private static CoursePublishedIntegrationEvent event() {
        return new CoursePublishedIntegrationEvent(COURSE, "Java Basics", List.of(
                new CoursePublishedIntegrationEvent.Lecture(LECTURE, "Intro", 1)));
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        CreateEnrollmentCourseCommandHandler createEnrollmentCourseCommandHandler() {
            return mock(CreateEnrollmentCourseCommandHandler.class);
        }

        @Bean
        CoursePublishedIntegrationEventHandler coursePublishedIntegrationEventHandler(CreateEnrollmentCourseCommandHandler handler) {
            return new CoursePublishedIntegrationEventHandler(handler);
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

    /**
     * Drives Spring's transaction synchronization without any resource, so after-commit semantics can be observed deterministically.
     */
    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

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
