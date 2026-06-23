# 14. Integration Event Error Handling

Date: 2024-01-01

## Status

Accepted

## Context

Async integration event handlers had no error handling. When a handler failed (e.g., due to a transient database connectivity issue or an optimistic locking failure), the exception was silently swallowed by the default Spring async exception handler. This meant:

- Failures were invisible — no logging, no alerting, no persistence.
- Transient errors that could succeed on retry were treated as permanent failures.
- There was no way to investigate or replay failed events after the fact.

## Decision

We add structured error handling to all integration event handlers:

1. **Custom `AsyncUncaughtExceptionHandler`** — logs uncaught async exceptions with full context (method name, parameters, stack trace) via SLF4J. A named `ThreadPoolTaskExecutor` with the `integration-event-` prefix improves log traceability.

2. **Retry with exponential backoff** — using Spring Retry (`@Retryable`) with:
   - Max 3 attempts
   - Exponential backoff (500ms initial, multiplier 2)
   - Retries only for transient/recoverable exceptions (`DataAccessException`, `ObjectOptimisticLockingFailureException`)
   - Business logic exceptions (e.g., `ResourceNotFoundException`) are NOT retried

3. **Structured logging** — each handler logs:
   - INFO on event received (with event details)
   - ERROR on failure (with event details and exception)

4. **Dead-letter persistence** — when all retries are exhausted, the `@Recover` method persists the failed event to a `failed_integration_events` table, recording: event class, payload, exception details, timestamp, retry count, and status. This allows operators to query, investigate, and manually retry failed events.

## Consequences

### Positive
- Transient failures are automatically recovered via retry.
- All failures are logged with full context for debugging.
- Permanently failed events are persisted for investigation and manual replay.
- Named thread pool prefix makes async event processing easily identifiable in logs.
- No silent data loss from integration event failures.

### Negative
- Added dependency on `spring-retry`.
- Retry introduces additional latency for transient failures (up to ~3.5s for 3 attempts with backoff).
- The `failed_integration_events` table requires periodic cleanup or archival.
- All event handler classes are now slightly larger due to logging and recovery code.
