"""Message broker abstraction replacing Spring's in-process ``ApplicationEventPublisher``/``@EventListener``.

``MessageBroker`` is the port; ``InMemoryMessageBroker`` is a synchronous implementation for tests and local
development, ``RabbitMQMessageBroker`` (``rabbitmq.py``) is the production implementation.
"""

from __future__ import annotations

import logging
from abc import ABC, abstractmethod
from collections import defaultdict
from collections.abc import Callable

log = logging.getLogger(__name__)

Payload = dict[str, object]
MessageHandler = Callable[[Payload], None]


class MessageBroker(ABC):
    @abstractmethod
    def publish(self, topic: str, payload: Payload) -> None:
        """Publish a JSON-serialisable payload to ``topic``."""

    @abstractmethod
    def subscribe(self, topic: str, handler: MessageHandler) -> None:
        """Register ``handler`` for messages on ``topic``."""

    @abstractmethod
    def start_consuming(self) -> None:
        """Block and dispatch messages to the subscribed handlers until ``close`` is called."""

    @abstractmethod
    def close(self) -> None: ...


class InMemoryMessageBroker(MessageBroker):
    """Dispatches synchronously on ``publish``; records everything published for assertions."""

    def __init__(self) -> None:
        self._handlers: dict[str, list[MessageHandler]] = defaultdict(list)
        self.published: list[tuple[str, Payload]] = []

    def publish(self, topic: str, payload: Payload) -> None:
        self.published.append((topic, payload))
        for handler in list(self._handlers.get(topic, [])):
            handler(payload)

    def subscribe(self, topic: str, handler: MessageHandler) -> None:
        self._handlers[topic].append(handler)

    def start_consuming(self) -> None:
        log.info("In-memory broker: nothing to consume, messages are dispatched on publish")

    def close(self) -> None:
        self._handlers.clear()
