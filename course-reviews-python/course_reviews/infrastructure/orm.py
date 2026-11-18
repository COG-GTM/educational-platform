"""Table definitions and imperative (classical) mappers keeping the domain free of ORM concerns."""

from __future__ import annotations

from sqlalchemy import Column, Float, ForeignKey, Integer, String, Table, Uuid
from sqlalchemy.orm import composite

from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.domain.value_objects import Comment, CourseRating
from course_reviews.infrastructure.database import Base

metadata = Base.metadata

reviewable_course_table = Table(
    "reviewable_course",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("original_course_id", Uuid, nullable=False),
)

reviewer_table = Table(
    "reviewer",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("username", String(100), nullable=False),
)

course_review_table = Table(
    "course_review",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("uuid", Uuid, nullable=False),
    Column("reviewer", Integer, ForeignKey("reviewer.id", name="reviewer_fkey"), nullable=False),
    Column("course", Integer, ForeignKey("reviewable_course.id", name="reviewable_course_fkey"), nullable=False),
    Column("rating", Float, nullable=False),
    Column("comment", String(100), nullable=True),
)

_mappers_started = False


def start_mappers() -> None:
    global _mappers_started
    if _mappers_started:
        return

    Base.registry.map_imperatively(ReviewableCourse, reviewable_course_table)
    Base.registry.map_imperatively(Reviewer, reviewer_table)
    Base.registry.map_imperatively(
        CourseReview,
        course_review_table,
        properties={
            "_rating_value": course_review_table.c.rating,
            "_comment_value": course_review_table.c.comment,
            "rating": composite(CourseRating, "_rating_value"),
            "comment": composite(Comment, "_comment_value"),
        },
    )
    _mappers_started = True
