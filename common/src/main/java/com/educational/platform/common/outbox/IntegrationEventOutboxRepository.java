package com.educational.platform.common.outbox;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link IntegrationEventOutboxEntry}.
 */
public interface IntegrationEventOutboxRepository extends JpaRepository<IntegrationEventOutboxEntry, UUID> {

    List<IntegrationEventOutboxEntry> findTop50ByStatusOrderByCreatedAtAsc(OutboxEntryStatus status);
}
