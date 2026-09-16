"""Value objects embedded into the Course aggregate (``@Embeddable`` records in Java)."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CourseRating:
    rating: float = 0.0


@dataclass(frozen=True)
class NumberOfStudents:
    number: int = 0
