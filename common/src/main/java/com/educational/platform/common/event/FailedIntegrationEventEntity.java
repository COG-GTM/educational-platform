package com.educational.platform.common.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Dead-letter record persisted when an asynchronous integration event handler has exhausted
 * all of its retry attempts. Maps to the {@code failed_integration_events} table.
 */
@Entity
@Table(name = "failed_integration_events")
public class FailedIntegrationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_class_name", nullable = false)
    private String eventClassName;

    @Lob
    @Column(name = "event_payload")
    private String eventPayload;

    @Lob
    @Column(name = "exception_message")
    private String exceptionMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FailedIntegrationEventStatus status;

    // for JPA
    protected FailedIntegrationEventEntity() {
    }

    public FailedIntegrationEventEntity(String eventClassName, String eventPayload, String exceptionMessage, int retryCount) {
        this.eventClassName = eventClassName;
        this.eventPayload = eventPayload;
        this.exceptionMessage = exceptionMessage;
        this.retryCount = retryCount;
        this.createdAt = Instant.now();
        this.status = FailedIntegrationEventStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public String getEventClassName() {
        return eventClassName;
    }

    public String getEventPayload() {
        return eventPayload;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public FailedIntegrationEventStatus getStatus() {
        return status;
    }

    public void markResolved() {
        this.status = FailedIntegrationEventStatus.RESOLVED;
    }
}
