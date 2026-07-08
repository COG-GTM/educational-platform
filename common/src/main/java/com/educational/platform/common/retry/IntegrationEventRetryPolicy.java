package com.educational.platform.common.retry;

public final class IntegrationEventRetryPolicy {

    public static final int MAX_ATTEMPTS = 3;
    public static final long INITIAL_DELAY_MS = 100L;
    public static final double MULTIPLIER = 2.0;
    public static final long MAX_DELAY_MS = 1000L;

    private IntegrationEventRetryPolicy() {
    }
}
