package com.educational.platform.common.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * A single integration event persisted in the transactional outbox.
 *
 * <p>Entries are written in the same transaction as the business change that produced the event
 * and are dispatched asynchronously by the {@link IntegrationEventOutboxRelay}.
 */
@Entity
@Table(name = "integration_event_outbox")
public class IntegrationEventOutboxEntry {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 500)
    private String eventType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEntryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected IntegrationEventOutboxEntry() {
    }

    public IntegrationEventOutboxEntry(String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEntryStatus.NEW;
        this.createdAt = Instant.now();
    }

    public void markProcessed() {
        this.status = OutboxEntryStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = OutboxEntryStatus.FAILED;
        this.processedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEntryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
