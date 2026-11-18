"""Ownership check used by the update endpoint (``CourseReviewChecker.java``)."""

from __future__ import annotations

from uuid import UUID

from course_reviews.infrastructure.repositories import CourseReviewRepository


class CourseReviewChecker:
    def __init__(self, course_review_repository: CourseReviewRepository) -> None:
        self._course_review_repository = course_review_repository

    def has_access(self, username: str, review_id: UUID) -> bool:
        return self._course_review_repository.is_reviewer(review_id, username)
