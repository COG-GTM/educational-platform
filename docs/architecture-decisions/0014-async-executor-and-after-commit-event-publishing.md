# 14. Async executor and after-commit event publishing.
Date: 2026-07-08

## Status
Accepted

## Context
The platform uses Spring integration events to communicate between bounded contexts. The previous implementation published some integration events outside of an active transaction and consumed them with an unbounded async listener setup, which could lead to lost events and unbounded thread usage. The outstanding todo from [0002-integration-events-implementation.md](0002-integration-events-implementation.md) needs to be resolved.

## Decision
We will use a dedicated bounded `ThreadPoolTaskExecutor` named `integrationEventExecutor` with `CallerRunsPolicy` back-pressure and an `AsyncUncaughtExceptionHandler` for integration event listeners.

Integration events will be published while a transaction is active and consumed with `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` plus `@Async("integrationEventExecutor")` so they are handled only after successful commit.

Integration listeners will use Spring Retry with exponential backoff and `@Recover` logging to provide bounded retry and explicit failure visibility.
