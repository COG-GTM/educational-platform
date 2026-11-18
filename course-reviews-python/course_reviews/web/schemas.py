"""Request/response models (``ReviewCourseRequest``, ``UpdateCourseReviewRequest``, ``CourseReviewCreatedResponse``)."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ReviewCourseRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    rating: float = Field(ge=0, le=5)
    comment: str | None = Field(default=None, max_length=100)


class UpdateCourseReviewRequest(BaseModel):
    model_config = ConfigDict(extra="ignore")

    rating: float = Field(ge=0, le=5)
    comment: str | None = Field(default=None, max_length=100)


class CourseReviewCreatedResponse(BaseModel):
    uuid: UUID


class ErrorResponse(BaseModel):
    """Mirrors the platform's ``GlobalExceptionHandler`` error body."""

    errors: list[str]
