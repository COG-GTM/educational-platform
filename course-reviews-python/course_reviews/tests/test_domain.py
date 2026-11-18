"""Tests for the ORM-free domain model (``CourseReview``, ``ReviewableCourse``, ``Reviewer``, value objects)."""

from __future__ import annotations

from dataclasses import FrozenInstanceError
from uuid import UUID, uuid4

import pytest

from course_reviews.domain import Comment, CourseRating, CourseReview, ReviewableCourse, Reviewer


def test_create_assigns_fresh_uuid_and_wraps_value_objects() -> None:
    # when
    review = CourseReview.create(course=1, reviewer=2, rating=3.5, comment="great")

    # then
    assert isinstance(review.uuid, UUID)
    assert review.to_identifier() == review.uuid
    assert review.id is None
    assert review.course == 1
    assert review.reviewer == 2
    assert review.rating == CourseRating(3.5)
    assert review.comment == Comment("great")


def test_create_twice_distinct_identifiers() -> None:
    first = CourseReview.create(course=1, reviewer=2, rating=3.5, comment=None)
    second = CourseReview.create(course=1, reviewer=2, rating=3.5, comment=None)

    assert first.to_identifier() != second.to_identifier()


def test_create_without_comment_comment_holds_none() -> None:
    review = CourseReview.create(course=1, reviewer=2, rating=0, comment=None)

    assert review.comment == Comment(None)
    assert review.comment.comment is None


def test_update_replaces_rating_and_comment_but_keeps_identity() -> None:
    # given
    review = CourseReview.create(course=1, reviewer=2, rating=3.5, comment="old")
    uuid = review.uuid

    # when
    review.update(1.0, None)

    # then
    assert review.rating == CourseRating(1.0)
    assert review.comment == Comment(None)
    assert review.uuid == uuid
    assert review.course == 1
    assert review.reviewer == 2


def test_value_objects_are_immutable_and_compared_by_value() -> None:
    rating = CourseRating(4.0)

    assert rating == CourseRating(4.0)
    assert rating != CourseRating(4.5)
    assert Comment("a") == Comment("a")
    with pytest.raises(FrozenInstanceError):
        rating.rating = 1.0  # type: ignore[misc]


def test_reviewable_course_local_id_unpersisted_value_error() -> None:
    course = ReviewableCourse(uuid4())

    assert course.id is None
    with pytest.raises(ValueError, match="not been persisted"):
        _ = course.local_id


def test_reviewable_course_local_id_persisted_returns_id() -> None:
    original = uuid4()
    course = ReviewableCourse(original)
    course.id = 7

    assert course.local_id == 7
    assert course.original_course_id == original


def test_reviewer_local_id_unpersisted_value_error() -> None:
    reviewer = Reviewer("username")

    assert reviewer.id is None
    assert reviewer.username == "username"
    with pytest.raises(ValueError, match="not been persisted"):
        _ = reviewer.local_id


def test_reviewer_local_id_persisted_returns_id() -> None:
    reviewer = Reviewer("username")
    reviewer.id = 3

    assert reviewer.local_id == 3
