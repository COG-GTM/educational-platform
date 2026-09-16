"""Teacher aggregate: local projection of a platform user with role TEACHER (``teacher/Teacher.java``)."""

from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from courses_py.application.teacher.create import CreateTeacherCommand


class Teacher:
    id: int | None
    username: str

    def __init__(self, command: CreateTeacherCommand) -> None:
        self.id = None
        self.username = command.username

    def to_identity(self) -> str:
        return self.username
