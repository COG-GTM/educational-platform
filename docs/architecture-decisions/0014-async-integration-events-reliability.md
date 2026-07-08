# 14. Async integration-events reliability: bounded executor, after-commit publishing, and retry.
Date: 2026-07-08

## Status
Accepted

## Context
Integration events are published in-process with Spring's `ApplicationEventPublisher` and consumed asynchronously with `@Async` `@EventListener` methods, enabled globally by `@EnableAsync` (see ADR-0002). Three reliability gaps existed in that setup:

- No custom `TaskExecutor` was configured, so `@EnableAsync` fell back to `SimpleAsyncTaskExecutor`, which spawns a new, unbounded thread per event — no pooling and no back-pressure under load. Exceptions thrown from `void` `@Async` listeners were also silently swallowed by Spring.
- Events were published while the producer's transaction was still open, so a listener could act on an event for a transaction that subsequently rolled back. ADR-0002 already recorded the outstanding intent that "integration events should be published after successful transaction".
- A transient failure in a downstream command handler caused the event to be dropped, with no retry and no record of the loss.

ADR-0010 and ADR-0011 accept eventually adopting Axon Framework, and a transactional outbox for cross-process durability remains desirable. Those are larger efforts; this ADR covers the in-process reliability improvements that are valuable independently of that migration.

## Decision
We harden the existing Spring-based messaging infrastructure rather than introducing new middleware:

- **Bounded executor.** Define a `ThreadPoolTaskExecutor` bean named `integrationEventExecutor` (in `AsyncConfig`, implementing `AsyncConfigurer`) with fixed core/max pool sizes, a bounded queue, a named thread prefix, and a `CallerRunsPolicy` rejection handler for back-pressure. Provide an `AsyncUncaughtExceptionHandler` (`LoggingAsyncUncaughtExceptionHandler`) that logs otherwise-swallowed exceptions from async listeners.
- **After-commit publishing.** Consume events with `@Async("integrationEventExecutor")` + `@TransactionalEventListener(phase = AFTER_COMMIT)`. Publishers keep calling `publishEvent(...)` inside their transaction, so `AFTER_COMMIT` semantics deliver events only after a successful commit and never for rolled-back transactions.
- **Retry with back-off.** Each listener delegates the command-handler invocation to a dedicated `@Retryable` collaborator (`*RetryableInvoker`) with a bounded attempt count and exponential back-off (centralized in `IntegrationEventRetryPolicy`). Retries run within the async post-commit thread. A `@Recover` method logs the event and exhausting exception at `ERROR` so exhausted events are not lost silently. Retry is enabled via `@EnableRetry` (`RetryConfig`) using Spring Retry.

## Consequences
- Async event processing is now bounded and back-pressured, escaped exceptions are logged, events are only delivered after commit, and transient downstream failures are retried before being surfaced.
- Because `@TransactionalEventListener(AFTER_COMMIT)` requires an active transaction at publish time, publishers must remain `@Transactional`; an event published with no active transaction is not delivered by default.
- Retry-exhaustion handling is log-based (in-process) — it does not provide durability across process restarts. Durable delivery (transactional outbox / Axon per ADR-0010 and ADR-0011) remains a separate, future decision.
```
- https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html
```
