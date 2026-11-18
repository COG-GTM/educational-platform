"""Resolves the authenticated user to a local ``Reviewer`` (``CurrentUserAsReviewer.java``).

The Java version reads the username from ``SecurityContextHolder``; here the username is injected
by the web layer's auth dependency.
"""

from __future__ import annotations

from course_reviews.domain.reviewer import Reviewer
from course_reviews.exceptions import RelatedResourceIsNotResolvedException
from course_reviews.infrastructure.repositories import ReviewerRepository


class CurrentUserAsReviewer:
    def __init__(self, reviewer_repository: ReviewerRepository, username: str) -> None:
        self._reviewer_repository = reviewer_repository
        self._username = username

    @property
    def username(self) -> str:
        return self._username

    def user_as_reviewer(self) -> Reviewer:
        reviewer = self._reviewer_repository.find_by_username(self._username)
        if reviewer is None:
            raise RelatedResourceIsNotResolvedException(f"Reviewer cannot be found by username = {self._username}")
        return reviewer
