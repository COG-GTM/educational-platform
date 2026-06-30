package com.educational.platform.common.event;

/**
 * Lifecycle status of a {@link FailedIntegrationEvent} entry in the dead-letter store.
 */
public enum FailedEventStatus {
    FAILED,
    RESOLVED
}
