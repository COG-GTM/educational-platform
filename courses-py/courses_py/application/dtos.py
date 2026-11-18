"""Read-side DTOs (``CourseDTO``, ``CourseLightDTO``, ``CurriculumItemDTO`` family). Serialized in camelCase."""

from __future__ import annotations

from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class _CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True, frozen=True)


class QuestionDTO(_CamelModel):
    content: str


class LectureDTO(_CamelModel):
    type: Literal["Lecture"] = "Lecture"
    uuid: UUID
    title: str
    description: str
    serial_number: int | None
    text: str | None


class QuizDTO(_CamelModel):
    type: Literal["Quiz"] = "Quiz"
    uuid: UUID
    title: str
    description: str
    serial_number: int | None
    questions: list[QuestionDTO] = Field(default_factory=list)


CurriculumItemDTO = Annotated[LectureDTO | QuizDTO, Field(discriminator="type")]


class CourseLightDTO(_CamelModel):
    uuid: UUID
    name: str
    description: str
    number_of_students: int


class CourseDTO(_CamelModel):
    uuid: UUID
    name: str
    description: str
    number_of_students: int
    curriculum_items: list[CurriculumItemDTO] = Field(default_factory=list)
