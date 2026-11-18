"""``course/create``: create commands, ``CourseFactory``, ``CurrentUserAsTeacher`` and the command handler."""

from __future__ import annotations

from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from courses_py.application.exceptions import RelatedResourceIsNotResolvedException
from courses_py.application.ports import CourseRepository, TeacherRepository
from courses_py.application.security import ROLE_TEACHER, CurrentUser, require_role
from courses_py.application.validation import NotBlank
from courses_py.domain.course import Course
from courses_py.domain.teacher import Teacher


class CreateQuestionCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    content: NotBlank


_CURRICULUM_ITEM_CONFIG = ConfigDict(alias_generator=to_camel, populate_by_name=True, frozen=True)
"""Curriculum items are posted as-is inside ``CreateCourseRequest``, so they must read camelCase JSON like Jackson."""


class CreateLectureCommand(BaseModel):
    model_config = _CURRICULUM_ITEM_CONFIG

    type: Literal["Lecture"] = "Lecture"
    title: str
    description: str
    serial_number: int
    text: str | None = None


class CreateQuizCommand(BaseModel):
    model_config = _CURRICULUM_ITEM_CONFIG

    type: Literal["Quiz"] = "Quiz"
    title: str
    description: str
    serial_number: int
    text: str | None = None
    questions: list[CreateQuestionCommand] = Field(default_factory=list)


CreateCurriculumItemCommand = Annotated[CreateLectureCommand | CreateQuizCommand, Field(discriminator="type")]


class CreateCourseCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    name: NotBlank
    description: NotBlank
    curriculum_items: list[CreateCurriculumItemCommand] | None = None


class CurrentUserAsTeacher:
    """Resolves the ``Teacher`` row of the authenticated user (``CurrentUserAsTeacher.java``)."""

    def __init__(self, teacher_repository: TeacherRepository, current_user: CurrentUser) -> None:
        self._teacher_repository = teacher_repository
        self._current_user = current_user

    def user_as_teacher(self) -> Teacher:
        username = self._current_user.principal().username
        teacher = self._teacher_repository.find_by_username(username)
        if teacher is None:
            raise RelatedResourceIsNotResolvedException(f"Teacher with username: {username} not found")
        return teacher


class CourseFactory:
    def __init__(self, current_user_as_teacher: CurrentUserAsTeacher) -> None:
        self._current_user_as_teacher = current_user_as_teacher

    def create_from(self, command: CreateCourseCommand) -> Course:
        CreateCourseCommand.model_validate(command.model_dump())
        teacher = self._current_user_as_teacher.user_as_teacher()
        if teacher.id is None:
            raise RelatedResourceIsNotResolvedException(f"Teacher with username: {teacher.username} is not persisted")
        return Course(command, teacher.id)


class CreateCourseCommandHandler:
    """``@PreAuthorize("hasRole('TEACHER')")``"""

    def __init__(self, course_repository: CourseRepository, course_factory: CourseFactory, current_user: CurrentUser):
        self._course_repository = course_repository
        self._course_factory = course_factory
        self._current_user = current_user

    def handle(self, command: CreateCourseCommand) -> UUID:
        require_role(self._current_user.principal(), ROLE_TEACHER)
        course = self._course_factory.create_from(command)
        self._course_repository.save(course)
        return course.to_identity()
