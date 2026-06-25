package com.educational.platform.common.event;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisting and querying failed integration events.
 */
public interface FailedIntegrationEventRepository extends JpaRepository<FailedIntegrationEventRecord, Long> {
}
