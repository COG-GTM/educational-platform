"""``IntegrationEventPublisher`` adapters.

``BrokerIntegrationEventPublisher`` hands an event to the broker immediately. ``AfterCommitIntegrationEventPublisher``
buffers events on the SQLAlchemy ``Session`` so that ``publish_pending_events`` can send them once the unit of work has
committed: a rolled-back request then publishes nothing (in-process stand-in for a transactional outbox).
"""

from __future__ import annotations

from sqlalchemy.orm import Session

from courses_py.infrastructure.messaging.broker import MessageBroker
from courses_py.integration_events.events import IntegrationEvent

PENDING_EVENTS_KEY = "courses_py.pending_integration_events"


def _as_integration_event(event: object) -> IntegrationEvent:
    if not isinstance(event, IntegrationEvent):
        raise TypeError(f"{type(event).__name__} is not an IntegrationEvent")
    return event


class BrokerIntegrationEventPublisher:
    def __init__(self, broker: MessageBroker) -> None:
        self._broker = broker

    def publish(self, event: object) -> None:
        integration_event = _as_integration_event(event)
        self._broker.publish(integration_event.TOPIC, integration_event.to_payload())


class AfterCommitIntegrationEventPublisher:
    def __init__(self, session: Session) -> None:
        self._session = session

    def publish(self, event: object) -> None:
        pending: list[IntegrationEvent] = self._session.info.setdefault(PENDING_EVENTS_KEY, [])
        pending.append(_as_integration_event(event))


def publish_pending_events(session: Session, broker: MessageBroker) -> None:
    pending: list[IntegrationEvent] = session.info.pop(PENDING_EVENTS_KEY, [])
    publisher = BrokerIntegrationEventPublisher(broker)
    for event in pending:
        publisher.publish(event)
