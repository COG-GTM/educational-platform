"""``InMemoryMessageBroker``, ``build_broker`` and the ``IntegrationEventPublisher`` adapters."""

from __future__ import annotations

from uuid import UUID

import pytest
from sqlalchemy.orm import Session, sessionmaker

from courses_py.config import Settings
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker, Payload
from courses_py.infrastructure.messaging.factory import build_broker
from courses_py.infrastructure.messaging.publisher import (
    PENDING_EVENTS_KEY,
    AfterCommitIntegrationEventPublisher,
    BrokerIntegrationEventPublisher,
    publish_pending_events,
)
from courses_py.infrastructure.messaging.rabbitmq import RabbitMQMessageBroker
from courses_py.integration_events.events import (
    CourseRatingRecalculatedIntegrationEvent,
    SendCourseToApproveIntegrationEvent,
)

COURSE_ID = UUID("123e4567-e89b-12d3-a456-426655440001")


def _settings(broker: str) -> Settings:
    return Settings(
        database_url="sqlite://", jwt_secret_key="k", broker=broker, rabbitmq_url="amqp://guest:guest@h/%2F"
    )


class TestInMemoryMessageBroker:
    def test_publish_dispatchesToSubscribersOfTopicOnly(self) -> None:
        sut = InMemoryMessageBroker()
        a: list[Payload] = []
        b: list[Payload] = []
        sut.subscribe("a", a.append)
        sut.subscribe("a", a.append)
        sut.subscribe("b", b.append)

        sut.publish("a", {"x": 1})

        assert a == [{"x": 1}, {"x": 1}]
        assert b == []

    def test_publish_noSubscribers_recordedOnly(self) -> None:
        sut = InMemoryMessageBroker()

        sut.publish("orphan", {"x": 1})

        assert sut.published == [("orphan", {"x": 1})]

    def test_publish_handlerRaises_propagatesToPublisher(self) -> None:
        sut = InMemoryMessageBroker()

        def failing(payload: Payload) -> None:
            raise RuntimeError("boom")

        sut.subscribe("a", failing)

        with pytest.raises(RuntimeError, match="boom"):
            sut.publish("a", {})
        assert sut.published == [("a", {})]

    def test_close_dropsSubscriptionsButKeepsHistory(self) -> None:
        sut = InMemoryMessageBroker()
        received: list[Payload] = []
        sut.subscribe("a", received.append)
        sut.publish("a", {"n": 1})

        sut.close()
        sut.publish("a", {"n": 2})

        assert received == [{"n": 1}]
        assert sut.published == [("a", {"n": 1}), ("a", {"n": 2})]

    def test_startConsuming_returnsImmediately(self) -> None:
        InMemoryMessageBroker().start_consuming()


class TestBuildBroker:
    def test_memory_inMemoryBroker(self) -> None:
        assert isinstance(build_broker(_settings("memory")), InMemoryMessageBroker)

    def test_rabbitmq_rabbitBrokerWithoutConnecting(self) -> None:
        sut = build_broker(_settings("rabbitmq"))

        assert isinstance(sut, RabbitMQMessageBroker)
        assert sut._parameters.host == "h"

    @pytest.mark.parametrize("broker", ["", "kafka", "Memory", "RabbitMQ"])
    def test_unknown_valueError(self, broker: str) -> None:
        with pytest.raises(ValueError, match=f"Unknown broker {broker!r}"):
            build_broker(_settings(broker))


class TestBrokerIntegrationEventPublisher:
    def test_publish_integrationEvent_topicAndCamelCasePayload(self) -> None:
        broker = InMemoryMessageBroker()
        sut = BrokerIntegrationEventPublisher(broker)

        sut.publish(SendCourseToApproveIntegrationEvent(course_id=COURSE_ID))
        sut.publish(CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_ID, rating=4.5))

        assert broker.published == [
            (topics.SEND_COURSE_TO_APPROVE, {"courseId": str(COURSE_ID)}),
            (topics.COURSE_RATING_RECALCULATED, {"courseId": str(COURSE_ID), "rating": 4.5}),
        ]

    @pytest.mark.parametrize("event", [object(), {"courseId": str(COURSE_ID)}, "courses.send-course-to-approve"])
    def test_publish_notAnIntegrationEvent_typeErrorAndNothingPublished(self, event: object) -> None:
        broker = InMemoryMessageBroker()

        with pytest.raises(TypeError, match="is not an IntegrationEvent"):
            BrokerIntegrationEventPublisher(broker).publish(event)

        assert broker.published == []


class TestAfterCommitIntegrationEventPublisher:
    def test_publish_bufferedOnSessionInOrder_nothingSentUntilFlushed(
        self, session_factory: sessionmaker[Session]
    ) -> None:
        broker = InMemoryMessageBroker()
        first = SendCourseToApproveIntegrationEvent(course_id=COURSE_ID)
        second = CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_ID, rating=1.0)

        with session_factory() as session:
            sut = AfterCommitIntegrationEventPublisher(session)
            sut.publish(first)
            sut.publish(second)

            assert session.info[PENDING_EVENTS_KEY] == [first, second]
            assert broker.published == []

            publish_pending_events(session, broker)

        assert broker.published == [
            (topics.SEND_COURSE_TO_APPROVE, {"courseId": str(COURSE_ID)}),
            (topics.COURSE_RATING_RECALCULATED, {"courseId": str(COURSE_ID), "rating": 1.0}),
        ]

    def test_publishPendingEvents_drainsBuffer_secondFlushSendsNothing(
        self, session_factory: sessionmaker[Session]
    ) -> None:
        broker = InMemoryMessageBroker()

        with session_factory() as session:
            AfterCommitIntegrationEventPublisher(session).publish(
                SendCourseToApproveIntegrationEvent(course_id=COURSE_ID)
            )
            publish_pending_events(session, broker)
            publish_pending_events(session, broker)

            assert PENDING_EVENTS_KEY not in session.info
        assert len(broker.published) == 1

    def test_publishPendingEvents_nothingPending_noop(self, session_factory: sessionmaker[Session]) -> None:
        broker = InMemoryMessageBroker()

        with session_factory() as session:
            publish_pending_events(session, broker)

        assert broker.published == []

    def test_publish_notAnIntegrationEvent_typeErrorAndNothingBuffered(
        self, session_factory: sessionmaker[Session]
    ) -> None:
        with session_factory() as session:
            with pytest.raises(TypeError, match="is not an IntegrationEvent"):
                AfterCommitIntegrationEventPublisher(session).publish({"courseId": str(COURSE_ID)})

            assert session.info.get(PENDING_EVENTS_KEY, []) == []

    def test_publish_separateSessions_independentBuffers(self, session_factory: sessionmaker[Session]) -> None:
        broker = InMemoryMessageBroker()

        with session_factory() as one, session_factory() as other:
            AfterCommitIntegrationEventPublisher(one).publish(SendCourseToApproveIntegrationEvent(course_id=COURSE_ID))
            publish_pending_events(other, broker)
            assert broker.published == []

            publish_pending_events(one, broker)
        assert len(broker.published) == 1
