package com.educational.platform.common.event;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry so that {@link org.springframework.retry.annotation.Retryable}
 * annotations on integration event handlers are honoured. Placed in the {@code common}
 * module under {@code com.educational.platform} so it is picked up by component scanning.
 */
@Configuration
@EnableRetry
public class RetryConfig {
}
