"""Value objects of the Course Reviews domain (``CourseRating.java``, ``Comment.java``)."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CourseRating:
    rating: float


@dataclass(frozen=True)
class Comment:
    comment: str | None
