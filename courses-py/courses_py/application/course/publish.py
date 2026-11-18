"""``course/publish``: ``PublishCourseCommand``, ``CourseTeacherChecker`` and the command handler."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict

from courses_py.application.exceptions import AccessDeniedException, ResourceNotFoundException
from courses_py.application.ports import CourseRepository
from courses_py.application.security import ROLE_TEACHER, CurrentUser, Principal, require_role


class PublishCourseCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID


class CourseTeacherChecker:
    """``@courseTeacherChecker.hasAccess(authentication, #c.uuid)``"""

    def __init__(self, course_repository: CourseRepository) -> None:
        self._course_repository = course_repository

    def has_access(self, principal: Principal, course_id: UUID) -> bool:
        return self._course_repository.is_teacher(course_id, principal.username)

    def check_access(self, principal: Principal, course_id: UUID) -> None:
        if not self.has_access(principal, course_id):
            raise AccessDeniedException("Access Denied")


class PublishCourseCommandHandler:
    """``@PreAuthorize("hasRole('TEACHER') and @courseTeacherChecker.hasAccess(authentication, #c.uuid)")``"""

    def __init__(
        self, repository: CourseRepository, course_teacher_checker: CourseTeacherChecker, current_user: CurrentUser
    ) -> None:
        self._repository = repository
        self._course_teacher_checker = course_teacher_checker
        self._current_user = current_user

    def handle(self, command: PublishCourseCommand) -> None:
        principal = self._current_user.principal()
        require_role(principal, ROLE_TEACHER)
        self._course_teacher_checker.check_access(principal, command.uuid)

        course = self._repository.find_by_uuid(command.uuid)
        if course is None:
            raise ResourceNotFoundException(f"Course with uuid: {command.uuid} not found")

        course.publish()
        self._repository.save(course)
