"""Port of ``edit/UpdateCourseReviewCommandHandlerTest.java``."""

from __future__ import annotations

from unittest.mock import Mock, create_autospec
from uuid import UUID

import pytest

from course_reviews.application.commands import ReviewCourseCommand, UpdateCourseReviewCommand
from course_reviews.application.current_user import CurrentUserAsReviewer
from course_reviews.application.factory import CourseReviewFactory
from course_reviews.application.handlers import UpdateCourseReviewCommandHandler
from course_reviews.application.validation import Validator
from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.domain.value_objects import Comment, CourseRating
from course_reviews.exceptions import ConstraintViolationException, ResourceNotFoundException
from course_reviews.infrastructure.event_bus import EventBus
from course_reviews.infrastructure.repositories import CourseReviewRepository, ReviewableCourseRepository
from course_reviews.integration_events.events import CourseRatingRecalculatedIntegrationEvent

COURSE_UUID = UUID("123e4567-e89b-12d3-a456-426655440001")


@pytest.fixture
def course_review_repository() -> Mock:
    return create_autospec(CourseReviewRepository, instance=True)


@pytest.fixture
def reviewable_course_repository() -> Mock:
    return create_autospec(ReviewableCourseRepository, instance=True)


@pytest.fixture
def current_user_as_reviewer() -> Mock:
    return create_autospec(CurrentUserAsReviewer, instance=True)


@pytest.fixture
def event_bus() -> EventBus:
    return EventBus()


@pytest.fixture
def published(event_bus: EventBus) -> list[CourseRatingRecalculatedIntegrationEvent]:
    events: list[CourseRatingRecalculatedIntegrationEvent] = []
    event_bus.subscribe(CourseRatingRecalculatedIntegrationEvent, events.append)
    return events


@pytest.fixture
def course_review_factory(current_user_as_reviewer: Mock, reviewable_course_repository: Mock) -> CourseReviewFactory:
    return CourseReviewFactory(Validator(), current_user_as_reviewer, reviewable_course_repository)


@pytest.fixture
def sut(
    course_review_repository: Mock, reviewable_course_repository: Mock, event_bus: EventBus
) -> UpdateCourseReviewCommandHandler:
    return UpdateCourseReviewCommandHandler(
        Validator(), course_review_repository, reviewable_course_repository, event_bus
    )


@pytest.fixture
def configure_course_review(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    current_user_as_reviewer: Mock,
    course_review_factory: CourseReviewFactory,
) -> CourseReview:
    reviewable_course = ReviewableCourse(COURSE_UUID)
    reviewable_course.id = 11
    reviewable_course_repository.find_by_original_course_id.return_value = reviewable_course
    reviewable_course_repository.find_by_id.return_value = reviewable_course

    reviewer = Reviewer("username")
    reviewer.id = 22
    current_user_as_reviewer.user_as_reviewer.return_value = reviewer

    course_review = course_review_factory.create_from(
        ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0, comment="comment")
    )
    course_review_repository.find_by_uuid.return_value = course_review
    course_review_repository.average_rating.return_value = 3.0
    return course_review


def test_handle_existing_course_review_review_saved_with_updated_fields(
    sut: UpdateCourseReviewCommandHandler,
    course_review_repository: Mock,
    configure_course_review: CourseReview,
    published: list[CourseRatingRecalculatedIntegrationEvent],
) -> None:
    # given
    uuid = configure_course_review.uuid
    command = UpdateCourseReviewCommand(uuid=uuid, rating=3.0, comment="updated comment")

    # when
    sut.handle(command)

    # then
    course_review_repository.save.assert_called_once()
    review = course_review_repository.save.call_args.args[0]
    assert review.uuid == uuid
    assert review.rating == CourseRating(3.0)
    assert review.comment == Comment("updated comment")
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=3.0)]


def test_handle_invalid_id_resource_not_found_exception(
    sut: UpdateCourseReviewCommandHandler,
    course_review_repository: Mock,
    published: list[CourseRatingRecalculatedIntegrationEvent],
) -> None:
    # given
    uuid = UUID("123e4567-e89b-12d3-a456-426655440001")
    command = UpdateCourseReviewCommand(uuid=uuid, rating=3.0, comment="updated comment")
    course_review_repository.find_by_uuid.return_value = None

    # when / then
    with pytest.raises(ResourceNotFoundException, match=str(uuid)):
        sut.handle(command)
    course_review_repository.save.assert_not_called()
    assert published == []


def test_handle_rating_empty_constraint_violation_exception(
    sut: UpdateCourseReviewCommandHandler,
    course_review_repository: Mock,
    configure_course_review: CourseReview,
) -> None:
    # given
    command = UpdateCourseReviewCommand.model_construct(
        uuid=configure_course_review.uuid, rating=None, comment="updated comment"
    )

    # when / then
    with pytest.raises(ConstraintViolationException):
        sut.handle(command)
    course_review_repository.save.assert_not_called()


@pytest.mark.parametrize("rating", [-0.5, 5.5])
def test_handle_rating_out_of_range_constraint_violation_exception(
    sut: UpdateCourseReviewCommandHandler,
    course_review_repository: Mock,
    configure_course_review: CourseReview,
    rating: float,
) -> None:
    # given
    command = UpdateCourseReviewCommand.model_construct(
        uuid=configure_course_review.uuid, rating=rating, comment="updated comment"
    )

    # when / then
    with pytest.raises(ConstraintViolationException):
        sut.handle(command)
    course_review_repository.save.assert_not_called()
