package com.educational.platform.common.event;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Persists failed integration events for later investigation and potential manual retry.
 */
@Entity
@Table(name = "failed_integration_events")
public class FailedIntegrationEventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_class_name", nullable = false)
    private String eventClassName;

    @Column(name = "event_payload", nullable = false, length = 4000)
    private String eventPayload;

    @Column(name = "exception_message", length = 2000)
    private String exceptionMessage;

    @Column(name = "exception_class_name")
    private String exceptionClassName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    protected FailedIntegrationEventRecord() {
    }

    public FailedIntegrationEventRecord(String eventClassName, String eventPayload, String exceptionMessage,
                                        String exceptionClassName, int retryCount) {
        this.eventClassName = eventClassName;
        this.eventPayload = eventPayload;
        this.exceptionMessage = exceptionMessage;
        this.exceptionClassName = exceptionClassName;
        this.createdAt = Instant.now();
        this.retryCount = retryCount;
        this.status = Status.FAILED;
    }

    public enum Status {
        FAILED, RESOLVED
    }
}
