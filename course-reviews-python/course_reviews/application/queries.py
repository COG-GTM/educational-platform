"""Read-side queries (``query/ListCourseReviewsByCourseUUIDQuery.java``)."""

from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict


class ListCourseReviewsByCourseUUIDQuery(BaseModel):
    model_config = ConfigDict(frozen=True)

    course_uuid: UUID
