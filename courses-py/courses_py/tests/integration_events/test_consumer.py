"""Ports of the ``*IntegrationEventHandlerTest`` classes: broker messages are routed to the command handlers."""

from __future__ import annotations

from uuid import UUID

import pytest
from pydantic import ValidationError
from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.course.create import CreateCourseCommand
from courses_py.domain.course import Course
from courses_py.domain.enums import ApprovalStatus
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, SqlAlchemyTeacherRepository
from courses_py.integration_events.consumer import IntegrationEventHandlers, register_consumers
from courses_py.integration_events.events import (
    INBOUND_EVENTS,
    CourseApprovedByAdminIntegrationEvent,
    CourseRatingRecalculatedIntegrationEvent,
    SendCourseToApproveIntegrationEvent,
    StudentEnrolledToCourseIntegrationEvent,
    UserCreatedIntegrationEvent,
)
from courses_py.tests.conftest import count, insert_teacher


@pytest.fixture
def draft_course_uuid(session_factory: sessionmaker[Session]) -> UUID:
    with session_factory() as session:
        teacher = insert_teacher(session)
        assert teacher.id is not None
        course = Course(CreateCourseCommand(name="name", description="description"), teacher.id)
        session.add(course)
        session.commit()
        return course.uuid


@pytest.fixture
def consuming_broker(session_factory: sessionmaker[Session]) -> InMemoryMessageBroker:
    broker = InMemoryMessageBroker()
    register_consumers(broker, session_factory)
    return broker


def _course(session_factory: sessionmaker[Session], uuid: UUID) -> Course:
    with session_factory() as session:
        course = SqlAlchemyCourseRepository(session).find_by_uuid(uuid)
        assert course is not None
        return course


def test_courseApprovedByAdmin_courseApproved(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    consuming_broker.publish(topics.COURSE_APPROVED_BY_ADMIN, {"courseId": str(draft_course_uuid)})

    assert _course(session_factory, draft_course_uuid).approval_status == ApprovalStatus.APPROVED


def test_studentEnrolledToCourse_numberOfStudentsIncreased(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    consuming_broker.publish(
        topics.STUDENT_ENROLLED_TO_COURSE, {"courseId": str(draft_course_uuid), "username": "student"}
    )

    assert _course(session_factory, draft_course_uuid).number_of_students.number == 1


def test_courseRatingRecalculated_ratingUpdated(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    consuming_broker.publish(topics.COURSE_RATING_RECALCULATED, {"courseId": str(draft_course_uuid), "rating": 3.5})

    assert _course(session_factory, draft_course_uuid).rating.rating == 3.5


def test_userCreated_teacherCreated(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session]
) -> None:
    consuming_broker.publish(topics.USER_CREATED, {"username": "new-user", "email": "new-user@example.com"})

    with session_factory() as session:
        assert SqlAlchemyTeacherRepository(session).find_by_username("new-user") is not None


def test_unknownCourse_handlerRaises(consuming_broker: InMemoryMessageBroker) -> None:
    with pytest.raises(Exception, match="not found"):
        consuming_broker.publish(topics.COURSE_APPROVED_BY_ADMIN, {"courseId": "123e4567-e89b-12d3-a456-426655440099"})


def test_studentEnrolledTwice_countedTwice(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    for username in ("a", "b"):
        consuming_broker.publish(
            topics.STUDENT_ENROLLED_TO_COURSE, {"courseId": str(draft_course_uuid), "username": username}
        )

    assert _course(session_factory, draft_course_uuid).number_of_students.number == 2


def test_courseRatingRecalculated_integerRatingCoercedToFloat(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    consuming_broker.publish(topics.COURSE_RATING_RECALCULATED, {"courseId": str(draft_course_uuid), "rating": 4})

    assert _course(session_factory, draft_course_uuid).rating.rating == 4.0


def test_userCreated_withoutEmail_teacherCreated(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session]
) -> None:
    consuming_broker.publish(topics.USER_CREATED, {"username": "no-email"})

    with session_factory() as session:
        assert SqlAlchemyTeacherRepository(session).find_by_username("no-email") is not None


def test_unknownPayloadKeys_ignored(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    consuming_broker.publish(
        topics.COURSE_APPROVED_BY_ADMIN, {"courseId": str(draft_course_uuid), "occurredOn": "2026-01-01", "x": 1}
    )

    assert _course(session_factory, draft_course_uuid).approval_status == ApprovalStatus.APPROVED


@pytest.mark.parametrize(
    ("topic", "payload"),
    [
        (topics.COURSE_APPROVED_BY_ADMIN, {}),
        (topics.COURSE_APPROVED_BY_ADMIN, {"courseId": "not-a-uuid"}),
        (topics.COURSE_APPROVED_BY_ADMIN, {"course_id_": "123e4567-e89b-12d3-a456-426655440001"}),
        (topics.STUDENT_ENROLLED_TO_COURSE, {"courseId": "123e4567-e89b-12d3-a456-426655440001"}),
        (topics.STUDENT_ENROLLED_TO_COURSE, {"username": "student"}),
        (topics.USER_CREATED, {"email": "x@example.com"}),
        (topics.USER_CREATED, {"username": None}),
        (topics.COURSE_RATING_RECALCULATED, {"courseId": "123e4567-e89b-12d3-a456-426655440001"}),
        (topics.COURSE_RATING_RECALCULATED, {"courseId": "123e4567-e89b-12d3-a456-426655440001", "rating": "high"}),
    ],
)
def test_malformedPayload_validationErrorAndNothingPersisted(
    consuming_broker: InMemoryMessageBroker,
    session_factory: sessionmaker[Session],
    topic: str,
    payload: dict[str, object],
) -> None:
    with pytest.raises(ValidationError):
        consuming_broker.publish(topic, payload)

    with session_factory() as session:
        assert count(session, "teacher") == 0 and count(session, "course") == 0


def test_unknownCourse_transactionRolledBack(
    consuming_broker: InMemoryMessageBroker, session_factory: sessionmaker[Session], draft_course_uuid: UUID
) -> None:
    with pytest.raises(Exception, match="not found"):
        consuming_broker.publish(
            topics.STUDENT_ENROLLED_TO_COURSE, {"courseId": str(UUID(int=99)), "username": "student"}
        )

    assert _course(session_factory, draft_course_uuid).number_of_students.number == 0


def test_registerConsumers_subscribesExactlyTheInboundTopics(session_factory: sessionmaker[Session]) -> None:
    broker = InMemoryMessageBroker()

    handlers = register_consumers(broker, session_factory)

    assert isinstance(handlers, IntegrationEventHandlers)
    assert set(broker._handlers) == {event.TOPIC for event in INBOUND_EVENTS}
    assert set(handlers.routes()) == set(broker._handlers)
    assert topics.SEND_COURSE_TO_APPROVE not in broker._handlers


def test_routes_topicsMatchJavaRoutingKeys(session_factory: sessionmaker[Session]) -> None:
    routes = IntegrationEventHandlers(session_factory).routes()

    assert routes == {
        "administration.course-approved-by-admin": routes[topics.COURSE_APPROVED_BY_ADMIN],
        "course-enrollments.student-enrolled-to-course": routes[topics.STUDENT_ENROLLED_TO_COURSE],
        "users.user-created": routes[topics.USER_CREATED],
        "course-reviews.course-rating-recalculated": routes[topics.COURSE_RATING_RECALCULATED],
    }
    assert SendCourseToApproveIntegrationEvent.TOPIC == "courses.send-course-to-approve"


def test_event_payloads_match_java_record_shape() -> None:
    uuid = UUID("123e4567-e89b-12d3-a456-426655440001")

    assert SendCourseToApproveIntegrationEvent(course_id=uuid).to_payload() == {"courseId": str(uuid)}
    assert CourseApprovedByAdminIntegrationEvent.model_validate({"courseId": str(uuid)}).course_id == uuid
    event = StudentEnrolledToCourseIntegrationEvent.model_validate({"courseId": str(uuid), "username": "u"})
    assert event.to_payload() == {"courseId": str(uuid), "username": "u"}


def test_event_payloads_remainingJavaRecordShapes() -> None:
    uuid = UUID("123e4567-e89b-12d3-a456-426655440001")

    rating = CourseRatingRecalculatedIntegrationEvent.model_validate({"courseId": str(uuid), "rating": 4.5})
    assert rating.to_payload() == {"courseId": str(uuid), "rating": 4.5}
    assert UserCreatedIntegrationEvent(username="u").to_payload() == {"username": "u", "email": None}
    assert UserCreatedIntegrationEvent(username="u", email="e").to_payload() == {"username": "u", "email": "e"}


def test_event_acceptsSnakeCaseAndIsFrozen() -> None:
    uuid = UUID("123e4567-e89b-12d3-a456-426655440001")

    event = CourseApprovedByAdminIntegrationEvent.model_validate({"course_id": str(uuid)})

    assert event.course_id == uuid
    with pytest.raises(ValidationError):
        event.course_id = UUID(int=0)  # type: ignore[misc]
