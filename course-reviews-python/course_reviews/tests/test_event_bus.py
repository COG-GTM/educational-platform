"""Tests for the synchronous in-process ``EventBus`` (``ApplicationEventPublisher`` replacement)."""

from __future__ import annotations

from dataclasses import dataclass
from uuid import uuid4

import pytest

from course_reviews.infrastructure.event_bus import EventBus
from course_reviews.integration_events.events import CourseRatingRecalculatedIntegrationEvent


@dataclass(frozen=True)
class OtherEvent:
    payload: str


@pytest.fixture
def sut() -> EventBus:
    return EventBus()


def test_publish_subscribed_handler_receives_event(sut: EventBus) -> None:
    # given
    received: list[CourseRatingRecalculatedIntegrationEvent] = []
    sut.subscribe(CourseRatingRecalculatedIntegrationEvent, received.append)
    event = CourseRatingRecalculatedIntegrationEvent(course_id=uuid4(), rating=4.5)

    # when
    sut.publish(event)

    # then
    assert received == [event]


def test_publish_no_subscribers_no_error(sut: EventBus) -> None:
    sut.publish(CourseRatingRecalculatedIntegrationEvent(course_id=uuid4(), rating=1.0))


def test_publish_unrelated_event_type_handler_not_invoked(sut: EventBus) -> None:
    # given
    received: list[CourseRatingRecalculatedIntegrationEvent] = []
    sut.subscribe(CourseRatingRecalculatedIntegrationEvent, received.append)

    # when
    sut.publish(OtherEvent("ignored"))

    # then
    assert received == []


def test_publish_multiple_handlers_invoked_in_subscription_order(sut: EventBus) -> None:
    # given
    calls: list[str] = []
    sut.subscribe(OtherEvent, lambda e: calls.append(f"first:{e.payload}"))
    sut.subscribe(OtherEvent, lambda e: calls.append(f"second:{e.payload}"))

    # when
    sut.publish(OtherEvent("x"))

    # then
    assert calls == ["first:x", "second:x"]


def test_publish_handler_subscribed_to_base_type_receives_subclass(sut: EventBus) -> None:
    # given
    received: list[object] = []
    sut.subscribe(object, received.append)
    event = OtherEvent("any")

    # when
    sut.publish(event)

    # then
    assert received == [event]


def test_unsubscribe_handler_no_longer_invoked(sut: EventBus) -> None:
    # given
    received: list[OtherEvent] = []
    sut.subscribe(OtherEvent, received.append)
    sut.unsubscribe(OtherEvent, received.append)

    # when
    sut.publish(OtherEvent("x"))

    # then
    assert received == []


def test_unsubscribe_unknown_handler_value_error(sut: EventBus) -> None:
    with pytest.raises(ValueError):
        sut.unsubscribe(OtherEvent, lambda e: None)


def test_publish_handler_unsubscribing_itself_during_publish_other_handlers_still_run(sut: EventBus) -> None:
    # given
    calls: list[str] = []

    def self_removing(event: OtherEvent) -> None:
        calls.append("self_removing")
        sut.unsubscribe(OtherEvent, self_removing)

    sut.subscribe(OtherEvent, self_removing)
    sut.subscribe(OtherEvent, lambda e: calls.append("second"))

    # when
    sut.publish(OtherEvent("x"))
    sut.publish(OtherEvent("y"))

    # then
    assert calls == ["self_removing", "second", "second"]


def test_publish_handler_raises_exception_propagates_to_publisher(sut: EventBus) -> None:
    # given
    def failing(_: OtherEvent) -> None:
        raise RuntimeError("boom")

    sut.subscribe(OtherEvent, failing)

    # when / then
    with pytest.raises(RuntimeError, match="boom"):
        sut.publish(OtherEvent("x"))
