package com.educational.platform.common.outbox;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the transactional outbox and dispatches stored integration events to in-process listeners.
 *
 * <p>Dispatch runs inside a transaction: each entry is published via
 * {@link ApplicationEventPublisher#publishEvent(Object)} and marked {@code PROCESSED} atomically, so the
 * {@code @TransactionalEventListener(AFTER_COMMIT)} consumers fire only once the entry is durably marked.
 * Entries whose payload can no longer be materialized (unknown or non-event type, malformed JSON) are
 * marked {@code FAILED} and logged for manual intervention.
 */
@Component
public class IntegrationEventOutboxRelay {

    static final long POLL_INTERVAL_MS = 1000L;
    static final String EVENT_TYPE_SUFFIX = "IntegrationEvent";
    static final String EVENT_PACKAGE_PREFIX = "com.educational.platform.";

    private static final Logger log = LoggerFactory.getLogger(IntegrationEventOutboxRelay.class);

    private final IntegrationEventOutboxRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntegrationEventOutboxRelay(IntegrationEventOutboxRepository repository,
                                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    @Transactional
    public void dispatchPendingEvents() {
        final List<IntegrationEventOutboxEntry> pending = repository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus.NEW);
        for (IntegrationEventOutboxEntry entry : pending) {
            try {
                eventPublisher.publishEvent(toEvent(entry));
                entry.markProcessed();
            } catch (Exception e) {
                log.error("Outbox entry [{}] of type [{}] could not be dispatched; marking FAILED. payload={}",
                        entry.getId(), entry.getEventType(), entry.getPayload(), e);
                entry.markFailed();
            }
            repository.save(entry);
        }
    }

    private Object toEvent(IntegrationEventOutboxEntry entry) throws Exception {
        final String eventType = entry.getEventType();
        if (!eventType.startsWith(EVENT_PACKAGE_PREFIX) || !eventType.endsWith(EVENT_TYPE_SUFFIX)) {
            throw new IllegalArgumentException("Type [" + eventType + "] is not an allowed integration event type");
        }
        final Class<?> eventClass = Class.forName(eventType);
        return objectMapper.readValue(entry.getPayload(), eventClass);
    }
}
