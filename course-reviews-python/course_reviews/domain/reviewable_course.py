"""Local projection of a course from the ``courses`` context (``course/ReviewableCourse.java``)."""

from __future__ import annotations

from uuid import UUID


class ReviewableCourse:
    id: int | None
    original_course_id: UUID

    def __init__(self, original_course_id: UUID) -> None:
        self.id = None
        self.original_course_id = original_course_id

    @property
    def local_id(self) -> int:
        """Database identity; only available once the entity has been persisted."""
        if self.id is None:
            raise ValueError("ReviewableCourse has not been persisted yet")
        return self.id
