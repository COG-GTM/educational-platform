"""Ports (interfaces) the application layer depends on; implemented under ``infrastructure/``."""

from __future__ import annotations

from typing import Protocol
from uuid import UUID

from courses_py.application.dtos import CourseDTO, CourseLightDTO
from courses_py.domain.course import Course
from courses_py.domain.teacher import Teacher


class CourseRepository(Protocol):
    """``CourseRepository`` + ``CourseRepositoryCustom``."""

    def save(self, course: Course) -> Course: ...

    def find_by_uuid(self, uuid: UUID) -> Course | None: ...

    def find_dto_by_uuid(self, uuid: UUID) -> CourseDTO | None: ...

    def list(self) -> list[CourseLightDTO]: ...

    def is_teacher(self, uuid: UUID, username: str) -> bool: ...


class TeacherRepository(Protocol):
    def save(self, teacher: Teacher) -> Teacher: ...

    def find_by_username(self, username: str) -> Teacher | None: ...


class IntegrationEventPublisher(Protocol):
    """Replacement for Spring's ``ApplicationEventPublisher`` for cross-process integration events."""

    def publish(self, event: object) -> None: ...
