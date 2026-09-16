"""Inbound integration-event handlers (port of the four ``*IntegrationEventHandler`` classes) and the consumer
process entry point (``courses-py-consumer``).
"""

from __future__ import annotations

import logging
from collections.abc import Callable

from sqlalchemy.orm import Session, sessionmaker

from courses_py.application.course.approve import ApproveCourseCommand, ApproveCourseCommandHandler
from courses_py.application.course.number_of_students import (
    IncreaseNumberOfStudentsCommand,
    IncreaseNumberOfStudentsCommandHandler,
)
from courses_py.application.course.rating import UpdateCourseRatingCommand, UpdateCourseRatingCommandHandler
from courses_py.application.teacher.create import CreateTeacherCommand, CreateTeacherCommandHandler
from courses_py.config import settings
from courses_py.infrastructure.messaging.broker import MessageBroker, Payload
from courses_py.infrastructure.messaging.factory import build_broker
from courses_py.infrastructure.persistence.database import build_engine, build_session_factory, transactional
from courses_py.infrastructure.persistence.orm import start_mappers
from courses_py.infrastructure.persistence.repositories import SqlAlchemyCourseRepository, SqlAlchemyTeacherRepository
from courses_py.integration_events.events import (
    CourseApprovedByAdminIntegrationEvent,
    CourseRatingRecalculatedIntegrationEvent,
    StudentEnrolledToCourseIntegrationEvent,
    UserCreatedIntegrationEvent,
)

log = logging.getLogger(__name__)


class IntegrationEventHandlers:
    def __init__(self, session_factory: sessionmaker[Session]) -> None:
        self._session_factory = session_factory

    def on_course_approved_by_admin(self, payload: Payload) -> None:
        """``CourseApprovedByAdminIntegrationEventHandler`` -> ``ApproveCourseCommandHandler``"""
        event = CourseApprovedByAdminIntegrationEvent.model_validate(payload)
        with transactional(self._session_factory) as session:
            ApproveCourseCommandHandler(SqlAlchemyCourseRepository(session)).handle(
                ApproveCourseCommand(uuid=event.course_id)
            )

    def on_student_enrolled_to_course(self, payload: Payload) -> None:
        """``StudentEnrolledToCourseIntegrationEventHandler`` -> ``IncreaseNumberOfStudentsCommandHandler``"""
        event = StudentEnrolledToCourseIntegrationEvent.model_validate(payload)
        with transactional(self._session_factory) as session:
            IncreaseNumberOfStudentsCommandHandler(SqlAlchemyCourseRepository(session)).handle(
                IncreaseNumberOfStudentsCommand(uuid=event.course_id)
            )

    def on_user_created(self, payload: Payload) -> None:
        """``UserCreatedIntegrationEventHandler`` -> ``CreateTeacherCommandHandler``"""
        event = UserCreatedIntegrationEvent.model_validate(payload)
        with transactional(self._session_factory) as session:
            CreateTeacherCommandHandler(SqlAlchemyTeacherRepository(session)).handle(
                CreateTeacherCommand(username=event.username)
            )

    def on_course_rating_recalculated(self, payload: Payload) -> None:
        """``CourseRatingRecalculatedIntegrationEventHandler`` -> ``UpdateCourseRatingCommandHandler``"""
        event = CourseRatingRecalculatedIntegrationEvent.model_validate(payload)
        with transactional(self._session_factory) as session:
            UpdateCourseRatingCommandHandler(SqlAlchemyCourseRepository(session)).handle(
                UpdateCourseRatingCommand(uuid=event.course_id, rating=event.rating)
            )

    def routes(self) -> dict[str, Callable[[Payload], None]]:
        return {
            CourseApprovedByAdminIntegrationEvent.TOPIC: self.on_course_approved_by_admin,
            StudentEnrolledToCourseIntegrationEvent.TOPIC: self.on_student_enrolled_to_course,
            UserCreatedIntegrationEvent.TOPIC: self.on_user_created,
            CourseRatingRecalculatedIntegrationEvent.TOPIC: self.on_course_rating_recalculated,
        }


def register_consumers(broker: MessageBroker, session_factory: sessionmaker[Session]) -> IntegrationEventHandlers:
    handlers = IntegrationEventHandlers(session_factory)
    for topic, handler in handlers.routes().items():
        broker.subscribe(topic, handler)
    return handlers


def main() -> None:
    """Entry point of the standalone consumer process (RabbitMQ)."""
    logging.basicConfig(level=logging.INFO)
    start_mappers()
    broker = build_broker(settings)
    register_consumers(broker, build_session_factory(build_engine(settings.database_url)))
    try:
        broker.start_consuming()
    except KeyboardInterrupt:
        pass
    finally:
        broker.close()
