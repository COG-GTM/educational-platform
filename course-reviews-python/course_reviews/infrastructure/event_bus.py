"""Minimal synchronous in-process event bus replacing Spring's ``ApplicationEventPublisher``."""

from __future__ import annotations

from collections import defaultdict
from collections.abc import Callable
from typing import Any, TypeVar

E = TypeVar("E")
Handler = Callable[[Any], None]


class EventBus:
    def __init__(self) -> None:
        self._handlers: dict[type, list[Handler]] = defaultdict(list)

    def subscribe(self, event_type: type[E], handler: Callable[[E], None]) -> None:
        self._handlers[event_type].append(handler)

    def unsubscribe(self, event_type: type[E], handler: Callable[[E], None]) -> None:
        self._handlers[event_type].remove(handler)

    def publish(self, event: object) -> None:
        for event_type, handlers in list(self._handlers.items()):
            if isinstance(event, event_type):
                for handler in list(handlers):
                    handler(event)
