"""Port of ``CourseReviewFactoryTest.java``."""

from __future__ import annotations

from unittest.mock import Mock, create_autospec
from uuid import UUID

import pytest

from course_reviews.application.commands import ReviewCourseCommand
from course_reviews.application.current_user import CurrentUserAsReviewer
from course_reviews.application.factory import CourseReviewFactory
from course_reviews.application.validation import Validator
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.domain.value_objects import Comment, CourseRating
from course_reviews.exceptions import ConstraintViolationException, RelatedResourceIsNotResolvedException
from course_reviews.infrastructure.repositories import ReviewableCourseRepository

COURSE_UUID = UUID("123e4567-e89b-12d3-a456-426655440001")


@pytest.fixture
def reviewable_course_repository() -> Mock:
    return create_autospec(ReviewableCourseRepository, instance=True)


@pytest.fixture
def current_user_as_reviewer() -> Mock:
    return create_autospec(CurrentUserAsReviewer, instance=True)


@pytest.fixture
def sut(current_user_as_reviewer: Mock, reviewable_course_repository: Mock) -> CourseReviewFactory:
    return CourseReviewFactory(Validator(), current_user_as_reviewer, reviewable_course_repository)


def test_create_from_valid_course_review_course_review_created(
    sut: CourseReviewFactory, reviewable_course_repository: Mock, current_user_as_reviewer: Mock
) -> None:
    # given
    command = ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0, comment="comment")

    course = ReviewableCourse(COURSE_UUID)
    course.id = 11
    reviewable_course_repository.find_by_original_course_id.return_value = course

    reviewer = Reviewer("username")
    reviewer.id = 22
    current_user_as_reviewer.user_as_reviewer.return_value = reviewer

    # when
    course_review = sut.create_from(command)

    # then
    assert course_review.rating == CourseRating(4.0)
    assert course_review.comment == Comment("comment")
    assert course_review.course == 11
    assert course_review.reviewer == 22
    assert isinstance(course_review.to_identifier(), UUID)
    reviewable_course_repository.find_by_original_course_id.assert_called_once_with(COURSE_UUID)


def test_create_from_course_id_is_null_constraint_violation_exception(sut: CourseReviewFactory) -> None:
    # given
    command = ReviewCourseCommand.model_construct(course_id=None, rating=4.0, comment="comment")

    # when / then
    with pytest.raises(ConstraintViolationException):
        sut.create_from(command)


@pytest.mark.parametrize("rating", [-1, 6])
def test_create_from_invalid_rating_constraint_violation_exception(sut: CourseReviewFactory, rating: float) -> None:
    # given
    command = ReviewCourseCommand.model_construct(course_id=COURSE_UUID, rating=rating, comment="comment")

    # when / then
    with pytest.raises(ConstraintViolationException):
        sut.create_from(command)


@pytest.mark.parametrize("rating", [0, 5])
def test_create_from_boundary_rating_accepted(
    sut: CourseReviewFactory, reviewable_course_repository: Mock, current_user_as_reviewer: Mock, rating: float
) -> None:
    # given
    course = ReviewableCourse(COURSE_UUID)
    course.id = 1
    reviewable_course_repository.find_by_original_course_id.return_value = course
    reviewer = Reviewer("username")
    reviewer.id = 2
    current_user_as_reviewer.user_as_reviewer.return_value = reviewer

    # when
    course_review = sut.create_from(ReviewCourseCommand(course_id=COURSE_UUID, rating=rating))

    # then
    assert course_review.rating == CourseRating(rating)
    assert course_review.comment == Comment(None)


def test_create_from_empty_rating_constraint_violation_exception(sut: CourseReviewFactory) -> None:
    # given
    command = ReviewCourseCommand.model_construct(course_id=COURSE_UUID, rating=None, comment="comment")

    # when / then
    with pytest.raises(ConstraintViolationException):
        sut.create_from(command)


def test_create_from_unresolved_course_related_resource_is_not_resolved_exception(
    sut: CourseReviewFactory, reviewable_course_repository: Mock
) -> None:
    # given
    command = ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0, comment="comment")
    reviewable_course_repository.find_by_original_course_id.return_value = None

    # when / then
    with pytest.raises(RelatedResourceIsNotResolvedException, match=str(COURSE_UUID)):
        sut.create_from(command)
