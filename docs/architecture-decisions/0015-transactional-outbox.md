# 15. Transactional outbox for integration events.
Date: 2026-07-09

## Status
Accepted

## Context
ADR-0014 hardened in-process event delivery (bounded executor, after-commit publishing, retry with back-off), but events still lived only in memory between the publishing transaction's commit and their asynchronous consumption. If the process died in that window — or while retries were still running — the event was lost with only an `ERROR` log as a trace. ADR-0002 and ADR-0014 both recorded durable delivery (a transactional outbox, or Axon per ADR-0010/ADR-0011) as an outstanding concern.

Adopting Axon Framework is a larger migration of the whole publish/subscribe mechanism. A transactional outbox achieves durability with the existing Spring infrastructure and does not preclude a later Axon migration.

## Decision
We introduce a transactional outbox in the **common** module:

- **`IntegrationEventOutbox`** replaces direct `ApplicationEventPublisher.publishEvent(...)` calls in command handlers. `publish(event)` serializes the event to JSON and stores it as an `IntegrationEventOutboxEntry` (`integration_event_outbox` table) with status `NEW`. The method requires an active transaction (`@Transactional(propagation = MANDATORY)`), so the entry commits — or rolls back — atomically with the business change that produced the event.
- **`IntegrationEventOutboxRelay`** polls `NEW` entries on a fixed schedule (`@Scheduled`, enabled by `SchedulingConfig`), materializes each payload back into its event class, and dispatches it via `ApplicationEventPublisher` inside a transaction, marking the entry `PROCESSED` in the same transaction. Only classes under `com.educational.platform.` whose names end in `IntegrationEvent` are materialized; entries that cannot be dispatched (unknown or disallowed type, malformed payload) are marked `FAILED` and logged for manual intervention.
- The consuming side is unchanged: listeners still use `@Async("integrationEventExecutor")` + `@TransactionalEventListener(phase = AFTER_COMMIT)` and retry with back-off per ADR-0014, now keyed off the relay's dispatch transaction.

## Consequences
- Events survive process restarts: an event is durable exactly when its producing transaction commits, and undelivered (`NEW`) entries are re-dispatched on startup.
- Dispatch by the relay is *at-least-once*: a crash before the relay's transaction commits re-dispatches the event, so listeners should be idempotent. Because `AFTER_COMMIT` listeners run only after the entry is already marked `PROCESSED`, a crash *during* listener execution does not re-dispatch — end-to-end processing of a dispatched event remains at-most-once, as it was before the outbox.
- Event delivery gains up to one poll interval of latency compared to direct in-memory publication.
- `FAILED` entries and `PROCESSED` history accumulate in the outbox table; cleanup/archival is left to operations.
- A future Axon migration (ADR-0010, ADR-0011) would replace both the outbox and the Spring event plumbing.
