"""RabbitMQ implementation of ``MessageBroker`` (topic exchange, one durable queue per subscribed topic)."""

from __future__ import annotations

import json
import logging
from collections.abc import Callable

import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exchange_type import ExchangeType
from pika.spec import Basic, BasicProperties

from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import MessageBroker, MessageHandler, Payload

log = logging.getLogger(__name__)


class RabbitMQMessageBroker(MessageBroker):
    def __init__(
        self,
        url: str,
        exchange: str = topics.EXCHANGE,
        queue_prefix: str = topics.CONSUMER_QUEUE_PREFIX,
        connection_factory: Callable[[pika.URLParameters], pika.BlockingConnection] = pika.BlockingConnection,
    ) -> None:
        self._parameters = pika.URLParameters(url)
        self._exchange = exchange
        self._queue_prefix = queue_prefix
        self._connection_factory = connection_factory
        self._connection: pika.BlockingConnection | None = None
        self._channel: BlockingChannel | None = None
        self._subscriptions: list[tuple[str, MessageHandler]] = []

    def _channel_or_connect(self) -> BlockingChannel:
        if self._channel is None or self._channel.is_closed:
            self._connection = self._connection_factory(self._parameters)
            self._channel = self._connection.channel()
            self._channel.exchange_declare(
                exchange=self._exchange,
                exchange_type=ExchangeType.topic,  # type: ignore[arg-type]  # types-pika declares members as str
                durable=True,
            )
        return self._channel

    def publish(self, topic: str, payload: Payload) -> None:
        channel = self._channel_or_connect()
        channel.basic_publish(
            exchange=self._exchange,
            routing_key=topic,
            body=json.dumps(payload).encode("utf-8"),
            properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
        )

    def subscribe(self, topic: str, handler: MessageHandler) -> None:
        channel = self._channel_or_connect()
        queue = self._queue_prefix + topic
        channel.queue_declare(queue=queue, durable=True)
        channel.queue_bind(queue=queue, exchange=self._exchange, routing_key=topic)

        def on_message(ch: BlockingChannel, method: Basic.Deliver, properties: BasicProperties, body: bytes) -> None:
            delivery_tag = method.delivery_tag or 0
            try:
                payload = json.loads(body)
                if not isinstance(payload, dict):
                    raise ValueError(f"expected a JSON object, got {type(payload).__name__}")
                handler(payload)
            except Exception:
                log.exception("Failed to handle message on %s, rejecting", topic)
                ch.basic_nack(delivery_tag=delivery_tag, requeue=False)
                return
            ch.basic_ack(delivery_tag=delivery_tag)

        channel.basic_consume(queue=queue, on_message_callback=on_message)
        self._subscriptions.append((topic, handler))

    def start_consuming(self) -> None:
        channel = self._channel_or_connect()
        log.info("Consuming %d topic(s) from exchange %s", len(self._subscriptions), self._exchange)
        channel.start_consuming()

    def close(self) -> None:
        if self._channel is not None and self._channel.is_open:
            self._channel.stop_consuming()
        if self._connection is not None and self._connection.is_open:
            self._connection.close()
        self._channel = None
        self._connection = None
