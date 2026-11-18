"""After-commit event publishing and RabbitMQ ack/nack semantics (without a broker)."""

from __future__ import annotations

from dataclasses import dataclass, field
from functools import partial
from uuid import uuid4

import pytest
from pydantic import ValidationError
from sqlalchemy.orm import Session, sessionmaker

from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker, Payload
from courses_py.infrastructure.messaging.publisher import AfterCommitIntegrationEventPublisher, publish_pending_events
from courses_py.infrastructure.messaging.rabbitmq import MAX_BODY_BYTES, RabbitMQMessageBroker
from courses_py.infrastructure.persistence.database import transactional
from courses_py.integration_events.events import (
    CourseApprovedByAdminIntegrationEvent,
    SendCourseToApproveIntegrationEvent,
)


def test_afterCommitPublisher_committed_eventPublished(
    session_factory: sessionmaker[Session], broker: InMemoryMessageBroker
) -> None:
    course_id = uuid4()

    with transactional(session_factory, partial(publish_pending_events, broker=broker)) as session:
        AfterCommitIntegrationEventPublisher(session).publish(SendCourseToApproveIntegrationEvent(course_id=course_id))
        assert broker.published == []

    assert broker.published == [(topics.SEND_COURSE_TO_APPROVE, {"courseId": str(course_id)})]


def test_afterCommitPublisher_rolledBack_nothingPublished(
    session_factory: sessionmaker[Session], broker: InMemoryMessageBroker
) -> None:
    with pytest.raises(RuntimeError):
        with transactional(session_factory, partial(publish_pending_events, broker=broker)) as session:
            AfterCommitIntegrationEventPublisher(session).publish(
                SendCourseToApproveIntegrationEvent(course_id=uuid4())
            )
            raise RuntimeError("commit never happens")

    assert broker.published == []


@dataclass
class FakeChannel:
    acked: list[int] = field(default_factory=list)
    nacked: list[tuple[int, bool]] = field(default_factory=list)

    def basic_ack(self, delivery_tag: int) -> None:
        self.acked.append(delivery_tag)

    def basic_nack(self, delivery_tag: int, requeue: bool) -> None:
        self.nacked.append((delivery_tag, requeue))


@dataclass
class FakeDeliver:
    delivery_tag: int = 7
    redelivered: bool = False


def _deliver(handler: object, body: bytes, redelivered: bool = False) -> FakeChannel:
    channel = FakeChannel()
    on_message = RabbitMQMessageBroker._on_message(topics.COURSE_APPROVED_BY_ADMIN, handler)
    on_message(channel, FakeDeliver(redelivered=redelivered), None, body)
    return channel


def test_onMessage_handled_acked() -> None:
    received: list[Payload] = []

    channel = _deliver(received.append, b'{"courseId": "x"}')

    assert received == [{"courseId": "x"}]
    assert channel.acked == [7] and channel.nacked == []


@pytest.mark.parametrize("body", [b"not json", b"[1, 2]", b"{" + b" " * MAX_BODY_BYTES + b"}"])
def test_onMessage_invalidBody_rejectedWithoutRequeue(body: bytes) -> None:
    channel = _deliver(lambda payload: None, body)

    assert channel.acked == [] and channel.nacked == [(7, False)]


def test_onMessage_validationError_rejectedWithoutRequeue() -> None:
    def handler(payload: Payload) -> None:
        CourseApprovedByAdminIntegrationEvent.model_validate(payload)

    channel = _deliver(handler, b'{"courseId": "not-a-uuid"}')

    assert channel.nacked == [(7, False)]
    with pytest.raises(ValidationError):
        handler({"courseId": "not-a-uuid"})


def test_onMessage_transientFailure_requeuedOnceThenRejected() -> None:
    def failing(payload: Payload) -> None:
        raise ConnectionError("database down")

    first = _deliver(failing, b'{"courseId": "x"}')
    second = _deliver(failing, b'{"courseId": "x"}', redelivered=True)

    assert first.nacked == [(7, True)]
    assert second.nacked == [(7, False)]
