"""Curriculum items of a course: ``CurriculumItem`` (abstract), ``Lecture``, ``Quiz`` and ``Question``.

Persisted with single-table inheritance on ``curriculum_item`` (discriminator column ``type``).
"""

from __future__ import annotations

from typing import TYPE_CHECKING
from uuid import UUID, uuid4

if TYPE_CHECKING:
    from courses_py.application.course.create import CreateLectureCommand, CreateQuizCommand
    from courses_py.domain.course import Course


class CurriculumItem:
    DISCRIMINATOR = "null"

    id: int | None
    uuid: UUID
    title: str
    description: str
    serial_number: int | None
    course: Course | None

    def __init__(self, title: str, description: str, course: Course | None, serial_number: int | None) -> None:
        self.id = None
        self.uuid = uuid4()
        self.title = title
        self.description = description
        self.course = course
        self.serial_number = serial_number


class Lecture(CurriculumItem):
    DISCRIMINATOR = "Lecture"

    content: str | None

    def __init__(self, command: CreateLectureCommand, serial_number: int | None, course: Course | None) -> None:
        super().__init__(command.title, command.description, course, serial_number)
        self.content = command.text


class Question:
    id: int | None
    content: str
    quiz: Quiz | None

    def __init__(self, content: str, quiz: Quiz | None) -> None:
        self.id = None
        self.content = content
        self.quiz = quiz


class Quiz(CurriculumItem):
    DISCRIMINATOR = "Quiz"

    questions: list[Question]

    def __init__(self, command: CreateQuizCommand, serial_number: int | None, course: Course | None) -> None:
        super().__init__(command.title, command.description, course, serial_number)
        self.questions = [Question(question.content, self) for question in command.questions]
