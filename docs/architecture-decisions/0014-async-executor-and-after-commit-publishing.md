# 14. Bounded async executor and after-commit integration event publishing
Date: 2026-07-08

## Status
Accepted

## Context
Integration events between bounded contexts are dispatched with Spring's `ApplicationEventPublisher` and consumed by `@Async @EventListener` methods (see [ADR-0002](0002-integration-events-implementation.md)). The original implementation had three shortcomings:

- **Unbounded async execution.** `@EnableAsync` was declared without a custom `TaskExecutor`, so Spring fell back to `SimpleAsyncTaskExecutor`, which spawns a brand new, unbounded thread for every event. Under load this can exhaust resources.
- **Events published inside a transaction were delivered even on rollback.** Command handlers published events while a transaction was still open (`@EventListener` fires synchronously at publish time). If the transaction later rolled back, other contexts had already reacted to an event that logically never happened. ADR-0002 explicitly left a `todo`: *"Integration events should be published after successful transaction."*
- **Silent failures.** Exceptions thrown by `void @Async` listeners are swallowed by Spring, and there was no retry, so a transient failure permanently lost the event.

## Decision
1. **Dedicated bounded executor.** Introduce `AsyncConfig` (in the `configuration` module) implementing `AsyncConfigurer`. It defines a `ThreadPoolTaskExecutor` bean named `integrationEventExecutor` with sensible core/max pool sizes, a bounded queue, a named thread prefix, and a `CallerRunsPolicy` rejection handler for back-pressure. It also provides an `AsyncUncaughtExceptionHandler` that logs exceptions thrown from `void` async listeners. Listeners reference the executor by name via `@Async("integrationEventExecutor")`.
2. **Publish after commit.** Command handlers publish integration events from within their transaction, and listeners are annotated with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` combined with `@Async`. Events are therefore processed only after the originating transaction commits successfully, resolving the outstanding `todo` from ADR-0002.
3. **Resilience.** Listener command-handler invocations are wrapped with Spring Retry (`@Retryable` with exponential backoff). Events that still fail after retries are exhausted are logged via a `@Recover` method so they are not lost silently.

A durable transactional outbox (aligned with ADR-0010 / ADR-0011 and Axon Framework) remains a possible future step for surviving process crashes between commit and async delivery; it is intentionally kept separate from this decision.

## Consequences
- Cross-context side effects no longer occur for rolled-back transactions.
- Async event processing is bounded and observable; failures are retried and, on final failure, logged instead of silently dropped.
- A new dependency, `org.springframework.retry:spring-retry`, is added to the modules that host integration event listeners (`courses-application`, `administration-application`) and to `configuration` (for `@EnableRetry`).
- Events are still held only in memory between publish and delivery, so a crash after commit but before delivery can still lose an event until an outbox is adopted.
