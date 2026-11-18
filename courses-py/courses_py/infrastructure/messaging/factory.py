from __future__ import annotations

from courses_py.config import Settings
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker, MessageBroker


def build_broker(config: Settings) -> MessageBroker:
    if config.broker == "rabbitmq":
        from courses_py.infrastructure.messaging.rabbitmq import RabbitMQMessageBroker

        return RabbitMQMessageBroker(config.rabbitmq_url)
    if config.broker == "memory":
        return InMemoryMessageBroker()
    raise ValueError(f"Unknown broker {config.broker!r}, expected 'memory' or 'rabbitmq'")
