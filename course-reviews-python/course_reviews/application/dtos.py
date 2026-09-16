"""Read-side DTOs (``CourseReviewDTO.java``)."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict


class CourseReviewDTO(BaseModel):
    model_config = ConfigDict(frozen=True)

    uuid: UUID
    course: UUID
    username: str
    comment: str | None
    rating: float
