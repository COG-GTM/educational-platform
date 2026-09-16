"""``IntegrationEventPublisher`` adapter: serialises an event and publishes it to its topic."""

from __future__ import annotations

from courses_py.infrastructure.messaging.broker import MessageBroker
from courses_py.integration_events.events import IntegrationEvent


class BrokerIntegrationEventPublisher:
    def __init__(self, broker: MessageBroker) -> None:
        self._broker = broker

    def publish(self, event: object) -> None:
        if not isinstance(event, IntegrationEvent):
            raise TypeError(f"{type(event).__name__} is not an IntegrationEvent")
        self._broker.publish(event.TOPIC, event.to_payload())
