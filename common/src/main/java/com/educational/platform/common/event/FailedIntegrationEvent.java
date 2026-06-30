package com.educational.platform.common.event;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Dead-letter record for an integration event whose handler exhausted all retries.
 *
 * <p>Persisted to the {@code failed_integration_events} table so failures are no longer
 * silently lost and operators can query and re-process them.
 */
@Entity
@Table(name = "failed_integration_events")
public class FailedIntegrationEvent {

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
    private Instant timestamp;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FailedEventStatus status;

    // for JPA
    protected FailedIntegrationEvent() {
    }

    private FailedIntegrationEvent(String eventClassName, String eventPayload, String exceptionMessage, int retryCount) {
        this.eventClassName = eventClassName;
        this.eventPayload = eventPayload;
        this.exceptionMessage = exceptionMessage;
        this.retryCount = retryCount;
        this.timestamp = Instant.now();
        this.status = FailedEventStatus.FAILED;
    }

    /**
     * Creates a new dead-letter entry in {@link FailedEventStatus#FAILED} state.
     *
     * @param eventClassName   fully-qualified class name of the failed integration event
     * @param eventPayload     the event serialized to JSON
     * @param exceptionMessage the message of the exception that caused the failure
     * @param retryCount       number of retry attempts that were exhausted
     */
    public static FailedIntegrationEvent of(String eventClassName, String eventPayload, String exceptionMessage, int retryCount) {
        return new FailedIntegrationEvent(eventClassName, eventPayload, exceptionMessage, retryCount);
    }

    /**
     * Marks this entry as resolved (e.g. after a successful manual re-processing).
     */
    public void resolve() {
        this.status = FailedEventStatus.RESOLVED;
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
}
