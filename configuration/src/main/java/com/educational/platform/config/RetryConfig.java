package com.educational.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry(order = Ordered.LOWEST_PRECEDENCE)
@Configuration
public class RetryConfig {
}
