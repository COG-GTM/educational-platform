package com.educational.platform.common.outbox;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
