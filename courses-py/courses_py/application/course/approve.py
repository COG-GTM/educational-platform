"""``course/approve``: ``ApproveCourseCommand(Handler)`` and ``SendCourseToApproveCommand(Handler)``."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict

from courses_py.application.course.publish import CourseTeacherChecker
from courses_py.application.exceptions import ResourceNotFoundException
from courses_py.application.ports import CourseRepository, IntegrationEventPublisher
from courses_py.application.security import ROLE_TEACHER, CurrentUser, require_role
from courses_py.integration_events.events import SendCourseToApproveIntegrationEvent


class ApproveCourseCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID


class ApproveCourseCommandHandler:
    def __init__(self, repository: CourseRepository) -> None:
        self._repository = repository

    def handle(self, command: ApproveCourseCommand) -> None:
        course = self._repository.find_by_uuid(command.uuid)
        if course is None:
            raise ResourceNotFoundException(f"Course with uuid: {command.uuid} not found")

        course.approve()
        self._repository.save(course)


class SendCourseToApproveCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID


class SendCourseToApproveCommandHandler:
    """``@PreAuthorize("hasRole('TEACHER') and @courseTeacherChecker.hasAccess(authentication, #c.uuid)")``

    Publishes ``SendCourseToApproveIntegrationEvent`` to the message broker so the (Java) administration module
    can create the course proposal.
    """

    def __init__(
        self,
        repository: CourseRepository,
        event_publisher: IntegrationEventPublisher,
        course_teacher_checker: CourseTeacherChecker,
        current_user: CurrentUser,
    ) -> None:
        self._repository = repository
        self._event_publisher = event_publisher
        self._course_teacher_checker = course_teacher_checker
        self._current_user = current_user

    def handle(self, command: SendCourseToApproveCommand) -> None:
        principal = self._current_user.principal()
        require_role(principal, ROLE_TEACHER)
        self._course_teacher_checker.check_access(principal, command.uuid)

        course = self._repository.find_by_uuid(command.uuid)
        if course is None:
            raise ResourceNotFoundException(f"Course with uuid: {command.uuid} not found")

        course.send_to_approve()
        self._repository.save(course)

        self._event_publisher.publish(SendCourseToApproveIntegrationEvent(course_id=command.uuid))
