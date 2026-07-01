package com.educational.platform.common.event;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for the {@link FailedIntegrationEventEntity} dead-letter store.
 */
public interface FailedIntegrationEventRepository extends JpaRepository<FailedIntegrationEventEntity, Long> {
}
