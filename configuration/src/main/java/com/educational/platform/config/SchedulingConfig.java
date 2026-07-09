package com.educational.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled tasks, in particular the transactional-outbox relay that dispatches
 * persisted integration events to in-process listeners.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
