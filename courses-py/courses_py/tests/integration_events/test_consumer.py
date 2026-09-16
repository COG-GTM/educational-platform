"""Ports of the ``*IntegrationEventHandlerTest`` classes: broker messages are routed to the command handlers."""

from __future__ import annotations

from uuid import UUID

import pytest
from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.course.create import CreateCourseCommand
from courses_py.domain.course import Course
from courses_py.domain.enums import ApprovalStatus
from courses_py.infrastructure.messaging import topics
from courses_py.infrastructure.messaging.broker import InMemoryMessageBroker
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, SqlAlchemyTeacherRepository
from courses_py.integration_events.consumer import register_consumers
from courses_py.integration_events.events import (
    CourseApprovedByAdminIntegrationEvent,
    SendCourseToApproveIntegrationEvent,
    StudentEnrolledToCourseIntegrationEvent,
)
from courses_py.tests.conftest import insert_teacher


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


def test_event_payloads_match_java_record_shape() -> None:
    uuid = UUID("123e4567-e89b-12d3-a456-426655440001")

    assert SendCourseToApproveIntegrationEvent(course_id=uuid).to_payload() == {"courseId": str(uuid)}
    assert CourseApprovedByAdminIntegrationEvent.model_validate({"courseId": str(uuid)}).course_id == uuid
    event = StudentEnrolledToCourseIntegrationEvent.model_validate({"courseId": str(uuid), "username": "u"})
    assert event.to_payload() == {"courseId": str(uuid), "username": "u"}
