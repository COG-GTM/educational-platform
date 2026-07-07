# 14. Bounded async executor and after-commit publishing for integration events.
Date: 2026-07-07

## Status
Accepted

## Context
Integration events are published with Spring's `ApplicationEventPublisher` and consumed by `@Async` `@EventListener` methods (see ADR-0002). This implementation had several weaknesses:
- `@EnableAsync` without a custom `TaskExecutor` falls back to `SimpleAsyncTaskExecutor`, which creates an unbounded new thread per event and provides no back-pressure;
- exceptions thrown from `@Async` void listener methods were silently swallowed;
- events were published while the surrounding transaction was still open, so listeners could react to changes that were later rolled back (the outstanding todo in ADR-0002);
- a single transient failure in a listener permanently lost the event.

## Decision
- A dedicated bounded `ThreadPoolTaskExecutor` bean named `integrationEventExecutor` (core 4, max 8, queue 100, thread prefix `integration-event-`, `CallerRunsPolicy` for back-pressure) is defined in `IntegrationEventsAsyncConfiguration`, which implements `AsyncConfigurer` so it is also the default `@Async` executor. Listeners reference it explicitly via `@Async("integrationEventExecutor")`.
- An `AsyncUncaughtExceptionHandler` logs exceptions (including the event arguments) that escape async listeners, so failed events are recorded instead of silently lost.
- Integration event listeners use `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)` instead of `@EventListener`, so events published within a transaction are only processed after a successful commit, while events published outside a transaction are still handled.
- Listener methods are annotated with Spring Framework's `@Retryable` (3 retries with exponential backoff, enabled via `@EnableResilientMethods`) to survive transient failures; exhausted retries propagate to the uncaught exception handler for logging.

A transactional outbox (e.g. via Axon, see ADR-0010/ADR-0011) would additionally make events durable across process crashes between commit and async delivery; that remains a possible future step and is intentionally out of scope here.

## Consequences
- Event processing throughput is bounded and predictable; bursts apply back-pressure to publishers instead of exhausting threads.
- Listeners never observe uncommitted or rolled-back state.
- Transient listener failures are retried; permanent failures are logged with the event payload.
- Events are still held in memory only: a crash between commit and async delivery loses the event (accepted until an outbox is introduced).
