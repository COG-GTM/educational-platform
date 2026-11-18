"""``course/rating/update``: ``UpdateCourseRatingCommand(Handler)``."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict

from courses_py.application.exceptions import ResourceNotFoundException
from courses_py.application.ports import CourseRepository


class UpdateCourseRatingCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID
    rating: float


class UpdateCourseRatingCommandHandler:
    def __init__(self, repository: CourseRepository) -> None:
        self._repository = repository

    def handle(self, command: UpdateCourseRatingCommand) -> None:
        course = self._repository.find_by_uuid(command.uuid)
        if course is None:
            raise ResourceNotFoundException(f"Course with uuid: {command.uuid} not found")

        course.update_rating(command.rating)
        self._repository.save(course)
