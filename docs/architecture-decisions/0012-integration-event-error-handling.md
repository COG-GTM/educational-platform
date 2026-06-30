# 12. Integration event error handling.
Date: 2026-06-30

## Status
Accepted

## Context
Cross-module communication relies on asynchronous integration-event handlers
(`@Async` Spring event listeners). Until now these handlers had no error
handling: an exception thrown from a fire-and-forget async method was swallowed
by the default `SimpleAsyncUncaughtExceptionHandler` and effectively lost, with
no logging, no retry and no record. Transient infrastructure failures (e.g. a
brief database outage or an optimistic-locking clash) permanently broke
inter-module consistency, and there was no way for operators to observe or
recover from a failed event.

## Decision
We will introduce a shared foundation for integration-event error handling in
the `common` and `configuration` modules:

- **Structured logging** via a custom `AsyncUncaughtExceptionHandler` that logs
  the failing method name, its parameters and the full stack trace at `ERROR`
  level, so async failures are always observable.
- **Retry with exponential backoff** using Spring Retry. `@EnableRetry` is
  switched on so handlers can annotate methods with `@Retryable`, and a reusable
  `IntegrationEventRetryHandler` exposes a `RetryTemplate` (max 3 attempts,
  initial 500ms interval, multiplier 2). Only transient/recoverable exceptions
  (`DataAccessException`, `OptimisticLockingFailureException` and subclasses) are
  retried; business exceptions are never retried.
- **Dead-letter persistence** via `@Recover` methods that store exhausted events
  in a new `failed_integration_events` table (entity `FailedIntegrationEvent`,
  repository `FailedIntegrationEventRepository`).
- **A dedicated thread pool** for async integration-event processing with the
  thread name prefix `integration-event-`.

## Consequences
- Inter-module communication becomes significantly more reliable (transient
  failures self-heal via retry) and observable (every failure is logged).
- A new `failed_integration_events` table is added via Liquibase; operators can
  query it and re-process or mark entries resolved.
- The ArchUnit "events should be immutable" rule is refined to exclude JPA
  `@Entity` classes, since persistence records (including `FailedIntegrationEvent`)
  are necessarily mutable and managed by the persistence provider.
- The five existing integration-event handlers are adopted incrementally in
  follow-up changes; this decision only establishes the shared building blocks.
