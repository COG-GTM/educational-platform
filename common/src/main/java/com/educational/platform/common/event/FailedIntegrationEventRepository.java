package com.educational.platform.common.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FailedIntegrationEventRepository extends JpaRepository<FailedIntegrationEventRecord, Long> {
}
