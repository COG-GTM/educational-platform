package com.educational.platform.common.outbox;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationEventOutboxRelayTest {

    @Mock
    private IntegrationEventOutboxRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void dispatchPendingEvents_newEntry_eventPublishedAndEntryProcessed() {
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
        final IntegrationEventOutboxEntry entry = new IntegrationEventOutboxEntry(
                SampleIntegrationEvent.class.getName(), "{\"uuid\":\"" + uuid + "\"}");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of(entry));

        sut.dispatchPendingEvents();

        verify(eventPublisher).publishEvent(new SampleIntegrationEvent(uuid));
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(OutboxEntryStatus.PROCESSED);
        assertThat(entry.getProcessedAt()).isNotNull();
    }

    @Test
    void dispatchPendingEvents_unknownEventType_entryMarkedFailed() {
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        final IntegrationEventOutboxEntry entry = new IntegrationEventOutboxEntry(
                "com.educational.platform.unknown.MissingIntegrationEvent", "{}");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of(entry));

        sut.dispatchPendingEvents();

        verify(eventPublisher, never()).publishEvent(any());
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(OutboxEntryStatus.FAILED);
    }

    @Test
    void dispatchPendingEvents_disallowedEventType_entryMarkedFailed() {
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        final IntegrationEventOutboxEntry entry = new IntegrationEventOutboxEntry(
                "java.lang.String", "\"payload\"");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of(entry));

        sut.dispatchPendingEvents();

        verify(eventPublisher, never()).publishEvent(any());
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(OutboxEntryStatus.FAILED);
    }

    @Test
    void dispatchPendingEvents_malformedPayload_entryMarkedFailed() {
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        final IntegrationEventOutboxEntry entry = new IntegrationEventOutboxEntry(
                SampleIntegrationEvent.class.getName(), "not-json");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of(entry));

        sut.dispatchPendingEvents();

        verify(eventPublisher, never()).publishEvent(any());
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(OutboxEntryStatus.FAILED);
    }

    @Test
    void dispatchPendingEvents_noPendingEntries_nothingPublishedOrSaved() {
        // given
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of());

        // when
        sut.dispatchPendingEvents();

        // then
        verify(eventPublisher, never()).publishEvent(any());
        verify(repository, never()).save(any());
    }

    @Test
    void dispatchPendingEvents_failingEntryInBatch_subsequentEntriesStillDispatched() {
        // given
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");
        final IntegrationEventOutboxEntry failing = new IntegrationEventOutboxEntry(
                SampleIntegrationEvent.class.getName(), "not-json");
        final IntegrationEventOutboxEntry dispatchable = new IntegrationEventOutboxEntry(
                SampleIntegrationEvent.class.getName(), "{\"uuid\":\"" + uuid + "\"}");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of(failing, dispatchable));

        // when
        sut.dispatchPendingEvents();

        // then
        verify(eventPublisher).publishEvent(new SampleIntegrationEvent(uuid));
        verify(repository).save(failing);
        verify(repository).save(dispatchable);
        assertThat(failing.getStatus()).isEqualTo(OutboxEntryStatus.FAILED);
        assertThat(dispatchable.getStatus()).isEqualTo(OutboxEntryStatus.PROCESSED);
    }

    @Test
    void dispatchPendingEvents_publisherThrows_entryMarkedFailedAndErrorLogged() {
        // given
        final IntegrationEventOutboxRelay sut = new IntegrationEventOutboxRelay(repository, eventPublisher);
        final UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426655440003");
        final IntegrationEventOutboxEntry entry = new IntegrationEventOutboxEntry(
                SampleIntegrationEvent.class.getName(), "{\"uuid\":\"" + uuid + "\"}");
        when(repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW)).thenReturn(List.of(entry));
        doThrow(new IllegalStateException("listener failed")).when(eventPublisher).publishEvent(new SampleIntegrationEvent(uuid));

        final Logger logger = (Logger) LoggerFactory.getLogger(IntegrationEventOutboxRelay.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            // when
            sut.dispatchPendingEvents();
        } finally {
            logger.detachAppender(appender);
        }

        // then
        verify(repository).save(entry);
        assertThat(entry.getStatus()).isEqualTo(OutboxEntryStatus.FAILED);
        assertThat(entry.getProcessedAt()).isNotNull();
        assertThat(appender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage())
                            .contains(entry.getId().toString())
                            .contains(SampleIntegrationEvent.class.getName());
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("listener failed");
                });
    }

    @Test
    void dispatchPendingEvents_isScheduledAndTransactional() throws Exception {
        // then
        assertThat(IntegrationEventOutboxRelay.class.isAnnotationPresent(Component.class)).isTrue();

        final Method method = IntegrationEventOutboxRelay.class.getMethod("dispatchPendingEvents");
        final Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelay()).isEqualTo(IntegrationEventOutboxRelay.POLL_INTERVAL_MS);
        assertThat(method.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
