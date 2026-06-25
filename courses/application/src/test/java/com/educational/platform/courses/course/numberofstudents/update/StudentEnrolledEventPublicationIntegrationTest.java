package com.educational.platform.courses.course.numberofstudents.update;

import com.educational.platform.common.event.FailedIntegrationEventRecord;
import com.educational.platform.common.event.FailedIntegrationEventRepository;
import com.educational.platform.course.enrollments.integration.event.StudentEnrolledToCourseIntegrationEvent;
import com.educational.platform.courses.course.numberofsudents.update.IncreaseNumberOfStudentsCommandHandler;
import com.educational.platform.courses.course.numberofsudents.update.StudentEnrolledToCourseIntegrationEventHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies that publishing a {@link StudentEnrolledToCourseIntegrationEvent} through the Spring
 * {@link ApplicationEventPublisher} actually triggers the {@code @EventListener} and drives the
 * {@code @Retryable}/{@code @Recover} dead-letter flow end-to-end. {@code @Async} is intentionally
 * not enabled so listener invocation is synchronous and deterministic.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StudentEnrolledEventPublicationIntegrationTest.PublicationTestConfig.class)
class StudentEnrolledEventPublicationIntegrationTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private IncreaseNumberOfStudentsCommandHandler commandHandler;

    @Autowired
    private FailedIntegrationEventRepository failedEventRepository;

    @Autowired
    private AtomicInteger invocationCounter;

    @BeforeEach
    void setUp() {
        Mockito.reset(commandHandler, failedEventRepository);
        invocationCounter.set(0);
    }

    @Test
    void eventPublished_successfulHandling_commandHandlerInvokedAndNoFailedEventPersisted() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "student");

        // when
        publisher.publishEvent(event);

        // then
        verify(commandHandler).handle(any());
        verify(failedEventRepository, never()).save(any());
    }

    @Test
    void eventPublished_retryableExceptionExhausted_recoverPersistsFailedEvent() {
        // given
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "student");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new QueryTimeoutException("connection pool exhausted");
        }).when(commandHandler).handle(any());

        // when
        publisher.publishEvent(event);

        // then - listener fired, retried up to maxAttempts, then recovered into the dead letter
        assertThat(invocationCounter.get()).isEqualTo(3);
        final ArgumentCaptor<FailedIntegrationEventRecord> captor = ArgumentCaptor.forClass(FailedIntegrationEventRecord.class);
        verify(failedEventRepository).save(captor.capture());
        final FailedIntegrationEventRecord record = captor.getValue();
        assertThat(record.getEventClassName()).isEqualTo(StudentEnrolledToCourseIntegrationEvent.class.getName());
        assertThat(record.getExceptionMessage()).isEqualTo("connection pool exhausted");
        assertThat(record.getRetryCount()).isEqualTo(3);
        assertThat(record.getStatus()).isEqualTo(FailedIntegrationEventRecord.FailedEventStatus.FAILED);
    }

    @Test
    void eventPublished_nonRetryableDataAccessException_notRetriedAndNotDeadLettered() {
        // given - DataIntegrityViolationException is a DataAccessException but NOT transient,
        // so it is excluded from retryFor and must never reach the transient-only @Recover.
        final StudentEnrolledToCourseIntegrationEvent event = new StudentEnrolledToCourseIntegrationEvent(UUID.randomUUID(), "student");
        doAnswer(invocation -> {
            invocationCounter.incrementAndGet();
            throw new DataIntegrityViolationException("constraint violation");
        }).when(commandHandler).handle(any());

        // when / then - handled exactly once (no retry), original error preserved, never dead-lettered
        assertThatThrownBy(() -> publisher.publishEvent(event))
                .hasRootCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(invocationCounter.get()).isEqualTo(1);
        verify(failedEventRepository, never()).save(any());
    }

    @Configuration
    @EnableRetry
    static class PublicationTestConfig {

        @Bean
        AtomicInteger invocationCounter() {
            return new AtomicInteger(0);
        }

        @Bean
        IncreaseNumberOfStudentsCommandHandler increaseNumberOfStudentsCommandHandler() {
            return mock(IncreaseNumberOfStudentsCommandHandler.class);
        }

        @Bean
        FailedIntegrationEventRepository failedIntegrationEventRepository() {
            return mock(FailedIntegrationEventRepository.class);
        }

        @Bean
        StudentEnrolledToCourseIntegrationEventHandler studentEnrolledToCourseIntegrationEventHandler(
                IncreaseNumberOfStudentsCommandHandler commandHandler,
                FailedIntegrationEventRepository repository) {
            return new StudentEnrolledToCourseIntegrationEventHandler(commandHandler, repository);
        }
    }
}
