package com.educational.platform.common.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persists failed integration events for dead-letter investigation and manual retry.
 */
@Entity
@Table(name = "failed_integration_events")
public class FailedIntegrationEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_class_name", nullable = false, length = 500)
    private String eventClassName;

    @Column(name = "event_payload", nullable = false, length = 2000)
    private String eventPayload;

    @Column(name = "exception_message", nullable = false, length = 2000)
    private String exceptionMessage;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FailedEventStatus status;

    protected FailedIntegrationEventRecord() {
    }

    public FailedIntegrationEventRecord(String eventClassName, String eventPayload, String exceptionMessage, int retryCount) {
        this.eventClassName = eventClassName;
        this.eventPayload = eventPayload;
        this.exceptionMessage = exceptionMessage;
        this.timestamp = Instant.now();
        this.retryCount = retryCount;
        this.status = FailedEventStatus.FAILED;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public FailedEventStatus getStatus() {
        return status;
    }

    public void resolve() {
        this.status = FailedEventStatus.RESOLVED;
    }

    public enum FailedEventStatus {
        FAILED, RESOLVED
    }
}
