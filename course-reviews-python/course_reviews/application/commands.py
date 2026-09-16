"""Write-side commands (``create/ReviewCourseCommand.java``, ``edit/UpdateCourseReviewCommand.java``).

Rating constraints mirror ``@PositiveOrZero @Max(5) @NotNull``. Commands are validated by the
``Validator`` (see ``validation.py``) inside the factory/handlers, as in the Java module; to build an
intentionally invalid command (e.g. in tests) use ``Model.model_construct(...)``.
"""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ReviewCourseCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    course_id: UUID
    rating: float = Field(ge=0, le=5)
    comment: str | None = Field(default=None, max_length=100)


class UpdateCourseReviewCommand(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID
    rating: float = Field(ge=0, le=5)
    comment: str | None = Field(default=None, max_length=100)
