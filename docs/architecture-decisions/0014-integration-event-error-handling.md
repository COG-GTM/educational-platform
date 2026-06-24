# 14. Integration Event Error Handling
Date: 2026-06-24

## Status
Done

## Context
Async integration event handlers (`@Async @EventListener`) had no error handling. When a handler threw an exception:
- The failure was silently swallowed by the default `SimpleAsyncUncaughtExceptionHandler`
- Transient errors (database connectivity, optimistic locking) were not retried
- There was no record of failed events for investigation or manual replay

This meant inter-module communication was unreliable and failures were invisible to operators.

## Decision
We will add structured error handling to all integration event handlers:

1. **Custom `AsyncUncaughtExceptionHandler`** — logs uncaught async exceptions with full context (method name, parameters, stack trace) via a dedicated `AsyncConfig` configuration class that also provides a named thread pool (`integration-event-` prefix) for traceability.

2. **Retry with exponential backoff** — using Spring Retry's `@Retryable` annotation on each handler method. Retries up to 3 attempts with exponential backoff (500ms initial, multiplier 2). Only transient/recoverable exceptions are retried (`TransientDataAccessException`, `OptimisticLockingFailureException`, `PessimisticLockingFailureException`). Business logic exceptions (e.g., `ResourceNotFoundException`) are not retried.

3. **Structured logging** — each handler logs `INFO` on event receipt and `ERROR` on failure with full event details and exception.

4. **Dead-letter persistence** — a `FailedIntegrationEvent` JPA entity stores events that exhaust all retries, including: event class name, payload, exception message, timestamp, retry count, and status (`FAILED`/`RESOLVED`). The `@Recover` method on each handler persists to this store.

## Consequences
- **Improved reliability**: transient failures are automatically retried before giving up
- **Improved observability**: all event processing is logged; failures are persisted for investigation
- **Manual retry capability**: operators can query `failed_integration_events` table and manually replay failed events
- **No retry on business errors**: `ResourceNotFoundException` and similar exceptions fail immediately without wasting retry attempts
- **Slight latency increase**: exponential backoff adds delay on transient failures (max ~3.5s for 3 attempts)
- **New dependency**: `spring-retry` and `spring-aspects` added to the build
