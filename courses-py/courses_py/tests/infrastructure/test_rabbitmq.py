"""``RabbitMQMessageBroker`` against a fake pika connection: topology, publishing, ack/nack and lifecycle."""

from __future__ import annotations

import json
import logging
from typing import Any

import pika
import pytest
from pika.exchange_type import ExchangeType
from pika.spec import Basic, BasicProperties

from courses_py.infrastructure.messaging import rabbitmq, topics
from courses_py.infrastructure.messaging.broker import Payload
from courses_py.infrastructure.messaging.rabbitmq import (
    MAX_BODY_BYTES,
    InvalidMessageError,
    RabbitMQMessageBroker,
    decode_payload,
)

URL = "amqp://guest:guest@rabbit.local:5672/%2F"


@pytest.fixture(autouse=True)
def enable_module_logger() -> None:
    # alembic's fileConfig() (test_migrations) disables loggers that already exist when it runs
    logging.getLogger(rabbitmq.__name__).disabled = False


class TestDecodePayload:
    def test_jsonObject_dict(self) -> None:
        assert decode_payload(b'{"courseId": "x", "rating": 4.5, "email": null}') == {
            "courseId": "x",
            "rating": 4.5,
            "email": None,
        }

    def test_emptyObject_emptyDict(self) -> None:
        assert decode_payload(b"{}") == {}

    def test_bodyAtSizeLimit_accepted(self) -> None:
        body = b'{"k": "' + b"a" * (MAX_BODY_BYTES - len(b'{"k": ""}')) + b'"}'
        assert len(body) == MAX_BODY_BYTES

        assert decode_payload(body) == {"k": "a" * (MAX_BODY_BYTES - 9)}

    def test_bodyOverSizeLimit_rejectedBeforeParsing(self) -> None:
        body = b"{" + b" " * MAX_BODY_BYTES + b"}"

        with pytest.raises(InvalidMessageError, match=f"exceeds {MAX_BODY_BYTES}"):
            decode_payload(body)

    @pytest.mark.parametrize("body", [b"", b"not json", b"{", b"\xff\xfe"])
    def test_notJson_invalidMessageError(self, body: bytes) -> None:
        with pytest.raises(InvalidMessageError, match="not valid JSON"):
            decode_payload(body)

    @pytest.mark.parametrize(
        ("body", "type_name"), [(b"[1]", "list"), (b'"s"', "str"), (b"1", "int"), (b"null", "NoneType")]
    )
    def test_jsonButNotObject_invalidMessageError(self, body: bytes, type_name: str) -> None:
        with pytest.raises(InvalidMessageError, match=f"expected a JSON object, got {type_name}"):
            decode_payload(body)

    def test_invalidMessageError_isValueError(self) -> None:
        assert issubclass(InvalidMessageError, ValueError)


class FakeChannel:
    def __init__(self) -> None:
        self.is_closed = False
        self.exchanges: list[dict[str, Any]] = []
        self.queues: list[dict[str, Any]] = []
        self.bindings: list[dict[str, Any]] = []
        self.published: list[dict[str, Any]] = []
        self.consumers: dict[str, Any] = {}
        self.acked: list[int] = []
        self.nacked: list[tuple[int, bool]] = []
        self.consuming_started = 0
        self.consuming_stopped = 0

    @property
    def is_open(self) -> bool:
        return not self.is_closed

    def exchange_declare(self, **kwargs: Any) -> None:
        self.exchanges.append(kwargs)

    def queue_declare(self, **kwargs: Any) -> None:
        self.queues.append(kwargs)

    def queue_bind(self, **kwargs: Any) -> None:
        self.bindings.append(kwargs)

    def basic_publish(self, **kwargs: Any) -> None:
        self.published.append(kwargs)

    def basic_consume(self, queue: str, on_message_callback: Any) -> None:
        self.consumers[queue] = on_message_callback

    def basic_ack(self, delivery_tag: int) -> None:
        self.acked.append(delivery_tag)

    def basic_nack(self, delivery_tag: int, requeue: bool) -> None:
        self.nacked.append((delivery_tag, requeue))

    def start_consuming(self) -> None:
        self.consuming_started += 1

    def stop_consuming(self) -> None:
        self.consuming_stopped += 1

    def deliver(self, queue: str, body: bytes, delivery_tag: int = 1, redelivered: bool = False) -> None:
        method = Basic.Deliver(delivery_tag=delivery_tag, redelivered=redelivered)
        self.consumers[queue](self, method, BasicProperties(), body)


class FakeConnection:
    def __init__(self, parameters: pika.URLParameters) -> None:
        self.parameters = parameters
        self.channels: list[FakeChannel] = []
        self.is_open = True

    def channel(self) -> FakeChannel:
        channel = FakeChannel()
        self.channels.append(channel)
        return channel

    def close(self) -> None:
        self.is_open = False


class FakeConnectionFactory:
    def __init__(self) -> None:
        self.connections: list[FakeConnection] = []

    def __call__(self, parameters: pika.URLParameters) -> FakeConnection:
        connection = FakeConnection(parameters)
        self.connections.append(connection)
        return connection

    @property
    def channel(self) -> FakeChannel:
        return self.connections[-1].channels[-1]


@pytest.fixture
def factory() -> FakeConnectionFactory:
    return FakeConnectionFactory()


@pytest.fixture
def sut(factory: FakeConnectionFactory) -> RabbitMQMessageBroker:
    return RabbitMQMessageBroker(URL, connection_factory=factory)


def test_init_doesNotConnect(factory: FakeConnectionFactory) -> None:
    RabbitMQMessageBroker(URL, connection_factory=factory)

    assert factory.connections == []


def test_publish_connectsAndDeclaresDurableTopicExchange(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory
) -> None:
    sut.publish(topics.SEND_COURSE_TO_APPROVE, {"courseId": "abc"})

    (connection,) = factory.connections
    assert connection.parameters.host == "rabbit.local"
    assert factory.channel.exchanges == [
        {"exchange": topics.EXCHANGE, "exchange_type": ExchangeType.topic, "durable": True}
    ]


def test_publish_persistentJsonMessageOnRoutingKey(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.publish(topics.SEND_COURSE_TO_APPROVE, {"courseId": "abc", "rating": 4.5})

    (message,) = factory.channel.published
    assert message["exchange"] == topics.EXCHANGE
    assert message["routing_key"] == topics.SEND_COURSE_TO_APPROVE
    assert json.loads(message["body"].decode("utf-8")) == {"courseId": "abc", "rating": 4.5}
    assert message["properties"].content_type == "application/json"
    assert message["properties"].delivery_mode == 2


def test_publish_twice_reusesOpenChannel(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.publish("a", {})
    sut.publish("b", {})

    assert len(factory.connections) == 1
    assert len(factory.channel.exchanges) == 1
    assert [m["routing_key"] for m in factory.channel.published] == ["a", "b"]


def test_publish_closedChannel_reconnects(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.publish("a", {})
    factory.channel.is_closed = True

    sut.publish("b", {})

    assert len(factory.connections) == 2
    assert [m["routing_key"] for m in factory.channel.published] == ["b"]


def test_subscribe_declaresDurablePrefixedQueueBoundToTopic(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory
) -> None:
    sut.subscribe(topics.USER_CREATED, lambda payload: None)

    channel = factory.channel
    queue = topics.CONSUMER_QUEUE_PREFIX + topics.USER_CREATED
    assert queue == "courses-py.users.user-created"
    assert channel.queues == [{"queue": queue, "durable": True}]
    assert channel.bindings == [{"queue": queue, "exchange": topics.EXCHANGE, "routing_key": topics.USER_CREATED}]
    assert list(channel.consumers) == [queue]


def test_subscribe_customExchangeAndPrefix(factory: FakeConnectionFactory) -> None:
    sut = RabbitMQMessageBroker(URL, exchange="other.exchange", queue_prefix="svc.", connection_factory=factory)

    sut.subscribe("topic", lambda payload: None)

    channel = factory.channel
    assert channel.exchanges[0]["exchange"] == "other.exchange"
    assert channel.queues == [{"queue": "svc.topic", "durable": True}]
    assert channel.bindings == [{"queue": "svc.topic", "exchange": "other.exchange", "routing_key": "topic"}]


def test_onMessage_validJson_handlerCalledAndAcked(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    received: list[Payload] = []
    sut.subscribe(topics.USER_CREATED, received.append)

    factory.channel.deliver("courses-py." + topics.USER_CREATED, b'{"username": "u", "email": null}', delivery_tag=7)

    assert received == [{"username": "u", "email": None}]
    assert factory.channel.acked == [7]
    assert factory.channel.nacked == []


def test_onMessage_handlerRaises_firstDeliveryRequeued(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory, caplog: pytest.LogCaptureFixture
) -> None:
    def failing(payload: Payload) -> None:
        raise RuntimeError("boom")

    sut.subscribe(topics.USER_CREATED, failing)

    with caplog.at_level("ERROR"):
        factory.channel.deliver("courses-py." + topics.USER_CREATED, b'{"username": "u"}', delivery_tag=3)

    assert factory.channel.nacked == [(3, True)]
    assert factory.channel.acked == []
    assert "Failed to handle message on users.user-created, requeueing once" in caplog.text


def test_onMessage_handlerRaisesOnRedelivery_nackedWithoutRequeue(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory, caplog: pytest.LogCaptureFixture
) -> None:
    def failing(payload: Payload) -> None:
        raise RuntimeError("boom")

    sut.subscribe(topics.USER_CREATED, failing)

    with caplog.at_level("ERROR"):
        factory.channel.deliver("courses-py." + topics.USER_CREATED, b'{"username": "u"}', 3, redelivered=True)

    assert factory.channel.nacked == [(3, False)]
    assert "Failed to handle message on users.user-created, rejecting" in caplog.text


def test_onMessage_handlerRaisesValueError_nackedWithoutRequeueEvenOnFirstDelivery(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory, caplog: pytest.LogCaptureFixture
) -> None:
    def rejecting(payload: Payload) -> None:
        raise ValueError("bad event")

    sut.subscribe(topics.USER_CREATED, rejecting)

    with caplog.at_level("ERROR"):
        factory.channel.deliver("courses-py." + topics.USER_CREATED, b'{"username": "u"}', delivery_tag=4)

    assert factory.channel.nacked == [(4, False)]
    assert "Invalid message on users.user-created, rejecting" in caplog.text


@pytest.mark.parametrize("body", [b"not json", b"[1, 2]", b'"string"', b"42"])
def test_onMessage_malformedBody_nackedAndHandlerNotCalled(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory, body: bytes
) -> None:
    received: list[Payload] = []
    sut.subscribe(topics.USER_CREATED, received.append)

    factory.channel.deliver("courses-py." + topics.USER_CREATED, body, delivery_tag=5)

    assert received == []
    assert factory.channel.nacked == [(5, False)]


def test_onMessage_missingDeliveryTag_acksTagZero(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.subscribe(topics.USER_CREATED, lambda payload: None)
    channel = factory.channel

    channel.consumers["courses-py." + topics.USER_CREATED](channel, Basic.Deliver(), BasicProperties(), b"{}")

    assert channel.acked == [0]


def test_subscribe_multipleTopics_dispatchedIndependently(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory
) -> None:
    a: list[Payload] = []
    b: list[Payload] = []
    sut.subscribe("a", a.append)
    sut.subscribe("b", b.append)

    factory.channel.deliver("courses-py.b", b'{"x": 1}')

    assert a == []
    assert b == [{"x": 1}]
    assert len(factory.connections) == 1


def test_startConsuming_blocksOnChannel(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory, caplog: pytest.LogCaptureFixture
) -> None:
    sut.subscribe("a", lambda payload: None)
    sut.subscribe("b", lambda payload: None)

    with caplog.at_level("INFO"):
        sut.start_consuming()

    assert factory.channel.consuming_started == 1
    assert "Consuming 2 topic(s) from exchange educational-platform.integration-events" in caplog.text


def test_close_stopsConsumingAndClosesConnection(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.subscribe("a", lambda payload: None)
    connection = factory.connections[0]
    channel = factory.channel

    sut.close()

    assert channel.consuming_stopped == 1
    assert connection.is_open is False


def test_close_neverConnected_noop(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.close()

    assert factory.connections == []


def test_close_alreadyClosedChannel_doesNotStopConsuming(
    sut: RabbitMQMessageBroker, factory: FakeConnectionFactory
) -> None:
    sut.publish("a", {})
    channel = factory.channel
    channel.is_closed = True

    sut.close()

    assert channel.consuming_stopped == 0
    assert factory.connections[0].is_open is False


def test_publish_afterClose_reconnects(sut: RabbitMQMessageBroker, factory: FakeConnectionFactory) -> None:
    sut.publish("a", {})
    sut.close()

    sut.publish("b", {})

    assert len(factory.connections) == 2
    assert [m["routing_key"] for m in factory.channel.published] == ["b"]
