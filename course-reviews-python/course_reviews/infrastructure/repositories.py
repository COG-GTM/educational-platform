"""Session-backed repositories (``CourseReviewRepository``, ``ReviewableCourseRepository``, ``ReviewerRepository``)."""

from __future__ import annotations

from uuid import UUID

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from course_reviews.application.dtos import CourseReviewDTO
from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.infrastructure.orm import course_review_table, reviewable_course_table, reviewer_table


class CourseReviewRepository:
    def __init__(self, session: Session) -> None:
        self._session = session

    def save(self, course_review: CourseReview) -> CourseReview:
        self._session.add(course_review)
        self._session.flush()
        return course_review

    def find_by_uuid(self, uuid: UUID) -> CourseReview | None:
        return self._session.scalars(select(CourseReview).where(course_review_table.c.uuid == uuid)).one_or_none()

    def list_course_reviews(self, course_uuid: UUID) -> list[CourseReviewDTO]:
        cr, c, r = course_review_table, reviewable_course_table, reviewer_table
        stmt = (
            select(cr.c.uuid, c.c.original_course_id, r.c.username, cr.c.comment, cr.c.rating)
            .join(c, cr.c.course == c.c.id)
            .join(r, cr.c.reviewer == r.c.id)
            .where(c.c.original_course_id == course_uuid)
            .order_by(cr.c.id)
        )
        return [
            CourseReviewDTO(uuid=uuid, course=course, username=username, comment=comment, rating=rating)
            for uuid, course, username, comment, rating in self._session.execute(stmt)
        ]

    def is_reviewer(self, uuid: UUID, username: str) -> bool:
        cr, r = course_review_table, reviewer_table
        stmt = (
            select(func.count())
            .select_from(cr)
            .join(r, cr.c.reviewer == r.c.id)
            .where(cr.c.uuid == uuid, r.c.username == username)
        )
        return (self._session.scalar(stmt) or 0) > 0

    def average_rating(self, course: int) -> float:
        stmt = select(func.avg(course_review_table.c.rating)).where(course_review_table.c.course == course)
        return float(self._session.scalar(stmt) or 0.0)


class ReviewableCourseRepository:
    def __init__(self, session: Session) -> None:
        self._session = session

    def save(self, course: ReviewableCourse) -> ReviewableCourse:
        self._session.add(course)
        self._session.flush()
        return course

    def find_by_id(self, id: int) -> ReviewableCourse | None:
        return self._session.get(ReviewableCourse, id)

    def find_by_original_course_id(self, uuid: UUID) -> ReviewableCourse | None:
        return self._session.scalars(
            select(ReviewableCourse).where(reviewable_course_table.c.original_course_id == uuid)
        ).one_or_none()


class ReviewerRepository:
    def __init__(self, session: Session) -> None:
        self._session = session

    def save(self, reviewer: Reviewer) -> Reviewer:
        self._session.add(reviewer)
        self._session.flush()
        return reviewer

    def find_by_username(self, username: str) -> Reviewer | None:
        return self._session.scalars(select(Reviewer).where(reviewer_table.c.username == username)).first()
