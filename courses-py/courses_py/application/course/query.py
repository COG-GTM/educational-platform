"""``course/query``: ``ListCourseQuery(Handler)`` and ``CourseByUUIDQuery(Handler)``."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict

from courses_py.application.dtos import CourseDTO, CourseLightDTO
from courses_py.application.ports import CourseRepository


class ListCourseQuery(BaseModel):
    model_config = ConfigDict(frozen=True)


class ListCourseQueryHandler:
    def __init__(self, repository: CourseRepository) -> None:
        self._repository = repository

    def handle(self, query: ListCourseQuery) -> list[CourseLightDTO]:
        return self._repository.list()


class CourseByUUIDQuery(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID


class CourseByUUIDQueryHandler:
    def __init__(self, repository: CourseRepository) -> None:
        self._repository = repository

    def handle(self, query: CourseByUUIDQuery) -> CourseDTO | None:
        return self._repository.find_dto_by_uuid(query.uuid)
