"""RabbitMQ implementation of ``MessageBroker`` (topic exchange, one durable queue per subscribed topic).

Delivery semantics: manual acks, so handlers run at-least-once. A message that cannot be parsed or fails validation is
rejected without requeue; a message whose handler fails for another reason (e.g. the database is down) is requeued
once (RabbitMQ's ``redelivered`` flag) and rejected on the second failure. Queues declare no dead-letter exchange;
configure one on the broker side if rejected messages must be kept.

Publishing uses publisher confirms with mandatory routing: ``publish`` returns only once the broker has confirmed the
message was routed to at least one queue, and raises ``pika.exceptions.UnroutableError`` (no queue bound to the topic,
e.g. the Java bridge is not running) or ``NackError`` otherwise. The caller decides how to surface that; the API
publishes after commit, so the request fails with 500 while the committed state change stands.

The blocking Pika connection is not thread-safe; all channel operations are serialised with a lock, since FastAPI
runs the synchronous routes that publish events on a worker thread pool.
"""

from __future__ import annotations

import json
import logging
import threading
from collections.abc import Callable

import pika
from pika.adapters.blocking_connection import BlockingChannel
from pika.exchange_type import ExchangeType
from pika.spec import Basic, BasicProperties

from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import MessageBroker, MessageHandler, Payload

log = logging.getLogger(__name__)

MAX_BODY_BYTES = 64 * 1024
"""Integration-event payloads are a handful of fields; larger bodies are rejected before parsing."""


class InvalidMessageError(ValueError):
    """The message can never be processed (oversized, not JSON, not an object, fails event validation)."""


def decode_payload(body: bytes) -> Payload:
    if len(body) > MAX_BODY_BYTES:
        raise InvalidMessageError(f"body of {len(body)} bytes exceeds {MAX_BODY_BYTES}")
    try:
        payload = json.loads(body)
    except ValueError as e:
        raise InvalidMessageError(f"body is not valid JSON: {e}") from e
    if not isinstance(payload, dict):
        raise InvalidMessageError(f"expected a JSON object, got {type(payload).__name__}")
    return payload


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
        self._lock = threading.RLock()

    def _channel_or_connect(self) -> BlockingChannel:
        if self._channel is None or self._channel.is_closed:
            self._connection = self._connection_factory(self._parameters)
            self._channel = self._connection.channel()
            self._channel.exchange_declare(
                exchange=self._exchange,
                exchange_type=ExchangeType.topic,  # type: ignore[arg-type]  # types-pika declares members as str
                durable=True,
            )
            self._channel.confirm_delivery()
        return self._channel

    def publish(self, topic: str, payload: Payload) -> None:
        with self._lock:
            channel = self._channel_or_connect()
            channel.basic_publish(
                exchange=self._exchange,
                routing_key=topic,
                body=json.dumps(payload).encode("utf-8"),
                properties=pika.BasicProperties(content_type="application/json", delivery_mode=2),
                mandatory=True,
            )

    def subscribe(self, topic: str, handler: MessageHandler) -> None:
        with self._lock:
            channel = self._channel_or_connect()
            queue = self._queue_prefix + topic
            channel.queue_declare(queue=queue, durable=True)
            channel.queue_bind(queue=queue, exchange=self._exchange, routing_key=topic)
            channel.basic_consume(queue=queue, on_message_callback=self._on_message(topic, handler))
            self._subscriptions.append((topic, handler))

    @staticmethod
    def _on_message(
        topic: str, handler: MessageHandler
    ) -> Callable[[BlockingChannel, Basic.Deliver, BasicProperties, bytes], None]:
        def on_message(ch: BlockingChannel, method: Basic.Deliver, properties: BasicProperties, body: bytes) -> None:
            delivery_tag = method.delivery_tag or 0
            try:
                handler(decode_payload(body))
            except ValueError:
                # includes InvalidMessageError and pydantic.ValidationError: the message itself is bad
                log.exception("Invalid message on %s, rejecting", topic)
                ch.basic_nack(delivery_tag=delivery_tag, requeue=False)
                return
            except Exception:
                requeue = not method.redelivered
                log.exception(
                    "Failed to handle message on %s, %s", topic, "requeueing once" if requeue else "rejecting"
                )
                ch.basic_nack(delivery_tag=delivery_tag, requeue=requeue)
                return
            ch.basic_ack(delivery_tag=delivery_tag)

        return on_message

    def start_consuming(self) -> None:
        with self._lock:
            channel = self._channel_or_connect()
        log.info("Consuming %d topic(s) from exchange %s", len(self._subscriptions), self._exchange)
        channel.start_consuming()

    def close(self) -> None:
        with self._lock:
            if self._channel is not None and self._channel.is_open:
                self._channel.stop_consuming()
            if self._connection is not None and self._connection.is_open:
                self._connection.close()
            self._channel = None
            self._connection = None
