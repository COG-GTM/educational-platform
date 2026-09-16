"""Integration events published by the Course Reviews context to other bounded contexts."""

from __future__ import annotations

from dataclasses import dataclass
from uuid import UUID


@dataclass(frozen=True)
class CourseRatingRecalculatedIntegrationEvent:
    """Published after a course rating is recalculated (``CourseRatingRecalculatedIntegrationEvent.java``)."""

    course_id: UUID
    rating: float
