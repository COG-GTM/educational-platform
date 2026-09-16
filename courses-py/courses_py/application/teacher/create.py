"""``teacher/create``: ``CreateTeacherCommand`` + ``CreateTeacherCommandHandler``."""

from __future__ import annotations

from pydantic import BaseModel, ConfigDict

from courses_py.application.ports import TeacherRepository
from courses_py.domain.teacher import Teacher


class CreateTeacherCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    username: str


class CreateTeacherCommandHandler:
    def __init__(self, teacher_repository: TeacherRepository) -> None:
        self._teacher_repository = teacher_repository

    def handle(self, command: CreateTeacherCommand) -> None:
        self._teacher_repository.save(Teacher(command))
