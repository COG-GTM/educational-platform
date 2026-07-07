# 14. Reliable asynchronous integration event processing
Date: 2026-07-07

## Status
Accepted

## Context
Integration events between bounded contexts are published via Spring's `ApplicationEventPublisher` and consumed by `@Async` `@EventListener` methods, with async processing enabled globally by `@EnableAsync`. This baseline (see [ADR 0002](0002-integration-events-implementation.md)) had three reliability gaps:

1. **Unbounded executor.** `@EnableAsync` had no custom `TaskExecutor`, so Spring fell back to `SimpleAsyncTaskExecutor`, which spawns a new, unbounded thread per event. Under load this risks thread exhaustion, and exceptions thrown from `@Async` listeners were silently swallowed.
2. **Events published inside the transaction.** Several command handlers published events while still inside their `@Transactional` boundary (e.g. `RegisterStudentToCourseCommandHandler`, `SendCourseToApproveCommandHandler`), so an event could be delivered even if the surrounding transaction later rolled back. This was the outstanding todo in ADR 0002.
3. **No failure handling.** A transient failure in a listener's command handler lost the event with no retry and no record.

## Decision
We keep the in-process Spring event mechanism (deferring the Axon-based durable outbox of [ADR 0010](0010-use-axon-framework.md) / [ADR 0011](0011-use-axon-event-publishing-mechanism.md)) and harden it as follows:

- **Bounded executor.** `AsyncConfig` implements `AsyncConfigurer` and provides a `ThreadPoolTaskExecutor` bean named `integrationEventExecutor` with bounded core/max pool sizes, a bounded queue, an `integration-event-` thread-name prefix, and a `CallerRunsPolicy` rejection handler for back-pressure. It also registers an `AsyncUncaughtExceptionHandler` that logs exceptions escaping async listeners.
- **After-commit publishing.** Consuming listeners use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` combined with `@Async("integrationEventExecutor")`. Publishers continue to publish inside the transaction so that events fire only after a successful commit, and never for rolled-back transactions.
- **Retry and recovery.** Command-handler invocations inside each listener are wrapped with Spring Retry (`@Retryable` with exponential backoff, `@EnableRetry`). Events that exhaust their retries are logged in a `@Recover` method so they are not silently lost, complementing the `AsyncUncaughtExceptionHandler`.

## Consequences
- Cross-context event processing no longer emits events for rolled-back work, tolerates transient failures, and cannot exhaust threads.
- Events still live only in memory between commit and delivery, so a process crash in that window can still lose an event. Durable delivery (transactional outbox / Axon) remains future work per ADR 0010 and ADR 0011.
