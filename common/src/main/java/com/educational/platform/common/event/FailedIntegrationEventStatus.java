package com.educational.platform.common.event;

/**
 * Lifecycle status of a {@link FailedIntegrationEventEntity} stored in the dead-letter table.
 */
public enum FailedIntegrationEventStatus {
    FAILED,
    RESOLVED
}
