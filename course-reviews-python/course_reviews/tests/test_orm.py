"""Tests for the imperative mapping in ``infrastructure/orm.py``."""

from __future__ import annotations

from sqlalchemy.orm import class_mapper

from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.domain.value_objects import Comment, CourseRating
from course_reviews.infrastructure.orm import (
    course_review_table,
    reviewable_course_table,
    reviewer_table,
    start_mappers,
)


def test_start_mappers_called_again_is_a_no_op() -> None:
    # given: mappers already started by the session fixture (and by create_app())
    mapper_before = class_mapper(CourseReview)

    # when: SQLAlchemy would raise ArgumentError on a second map_imperatively of the same class
    start_mappers()
    start_mappers()

    # then
    assert class_mapper(CourseReview) is mapper_before


def test_domain_classes_mapped_to_their_tables() -> None:
    assert class_mapper(CourseReview).local_table is course_review_table
    assert class_mapper(ReviewableCourse).local_table is reviewable_course_table
    assert class_mapper(Reviewer).local_table is reviewer_table


def test_course_review_value_objects_mapped_as_composites_over_rating_and_comment_columns() -> None:
    mapper = class_mapper(CourseReview)

    composites = {c.key: c for c in mapper.composites}
    assert set(composites) == {"rating", "comment"}
    assert composites["rating"].composite_class is CourseRating
    assert composites["rating"].attrs == ("_rating_value",)
    assert composites["comment"].composite_class is Comment
    assert composites["comment"].attrs == ("_comment_value",)

    columns = {attr.key: attr.columns[0] for attr in mapper.column_attrs}
    assert columns["_rating_value"] is course_review_table.c.rating
    assert columns["_comment_value"] is course_review_table.c.comment
