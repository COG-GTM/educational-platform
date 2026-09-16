"""REST DTOs (``CreateCourseRequest``, ``CreatedCourseResponse``, ``ErrorResponse``)."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from courses_py.application.course.create import CreateCurriculumItemCommand
from courses_py.application.validation import NotBlank


class CreateCourseRequest(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)

    name: NotBlank
    description: NotBlank
    curriculum_items: list[CreateCurriculumItemCommand] | None = None


class CreatedCourseResponse(BaseModel):
    uuid: UUID


class ErrorResponse(BaseModel):
    """``com.educational.platform.web.handler.ErrorResponse``: ``{"errors": ["..."]}``."""

    errors: list[str] = Field(default_factory=list)
