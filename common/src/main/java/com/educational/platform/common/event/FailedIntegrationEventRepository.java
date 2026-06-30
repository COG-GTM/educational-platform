package com.educational.platform.common.event;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the integration-event dead-letter store.
 */
public interface FailedIntegrationEventRepository extends JpaRepository<FailedIntegrationEvent, Long> {
}
