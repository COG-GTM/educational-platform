"""Tests for the ``Validator`` (``jakarta.validation.Validator`` analogue) and the command models."""

from __future__ import annotations

from uuid import uuid4

import pytest
from pydantic import ValidationError

from course_reviews.application.commands import ReviewCourseCommand, UpdateCourseReviewCommand
from course_reviews.application.validation import Validator
from course_reviews.exceptions import ConstraintViolationException


@pytest.fixture
def sut() -> Validator:
    return Validator()


def test_validate_valid_command_no_violations(sut: Validator) -> None:
    assert sut.validate(ReviewCourseCommand(course_id=uuid4(), rating=4.0, comment="c")) == []
    assert sut.validate(UpdateCourseReviewCommand(uuid=uuid4(), rating=0, comment=None)) == []


def test_validate_or_raise_valid_command_no_exception(sut: Validator) -> None:
    sut.validate_or_raise(ReviewCourseCommand(course_id=uuid4(), rating=5))


def test_validate_rating_above_max_violation_names_field(sut: Validator) -> None:
    # given
    command = ReviewCourseCommand.model_construct(course_id=uuid4(), rating=5.1, comment=None)

    # when
    violations = sut.validate(command)

    # then
    assert len(violations) == 1
    assert violations[0].startswith("rating: ")


def test_validate_multiple_invalid_fields_all_violations_reported(sut: Validator) -> None:
    # given
    command = ReviewCourseCommand.model_construct(course_id=None, rating=-1, comment=None)

    # when
    violations = sut.validate(command)

    # then
    assert len(violations) == 2
    assert {v.split(":")[0] for v in violations} == {"course_id", "rating"}


def test_validate_or_raise_invalid_command_exception_carries_violations(sut: Validator) -> None:
    # given
    command = UpdateCourseReviewCommand.model_construct(uuid=uuid4(), rating=None, comment=None)

    # when / then
    with pytest.raises(ConstraintViolationException) as exc_info:
        sut.validate_or_raise(command)
    assert exc_info.value.violations == sut.validate(command)
    assert str(exc_info.value) == "; ".join(exc_info.value.violations)


@pytest.mark.parametrize("rating", [-0.01, 5.01, None])
def test_review_course_command_invalid_rating_rejected_at_construction(rating: float | None) -> None:
    with pytest.raises(ValidationError):
        ReviewCourseCommand(course_id=uuid4(), rating=rating)


@pytest.mark.parametrize("rating", [0, 2.5, 5])
def test_update_course_review_command_rating_within_bounds_accepted(rating: float) -> None:
    command = UpdateCourseReviewCommand(uuid=uuid4(), rating=rating)

    assert command.rating == rating
    assert command.comment is None


def test_commands_are_immutable() -> None:
    command = ReviewCourseCommand(course_id=uuid4(), rating=1.0)

    with pytest.raises(ValidationError):
        command.rating = 2.0  # type: ignore[misc]
