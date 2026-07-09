package com.educational.platform.common.outbox;

/**
 * Lifecycle status of an {@link IntegrationEventOutboxEntry}.
 */
public enum OutboxEntryStatus {

    /**
     * Persisted together with the publishing transaction, awaiting dispatch.
     */
    NEW,

    /**
     * Dispatched to in-process listeners by the outbox relay.
     */
    PROCESSED,

    /**
     * Could not be dispatched (e.g. payload no longer deserializable); requires manual intervention.
     */
    FAILED
}
