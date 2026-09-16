"""Tests for the application handlers, ``CurrentUserAsReviewer`` and ``CourseReviewChecker`` not covered by the
``CourseReviewFactoryTest`` / ``UpdateCourseReviewCommandHandlerTest`` ports."""

from __future__ import annotations

from unittest.mock import Mock, create_autospec
from uuid import UUID, uuid4

import pytest

from course_reviews.application.checker import CourseReviewChecker
from course_reviews.application.commands import ReviewCourseCommand, UpdateCourseReviewCommand
from course_reviews.application.current_user import CurrentUserAsReviewer
from course_reviews.application.dtos import CourseReviewDTO
from course_reviews.application.factory import CourseReviewFactory
from course_reviews.application.handlers import (
    CreateReviewableCourseCommandHandler,
    CreateReviewerCommandHandler,
    ListCourseReviewsByCourseUUIDQueryHandler,
    ReviewCourseCommandHandler,
    UpdateCourseReviewCommandHandler,
)
from course_reviews.application.queries import ListCourseReviewsByCourseUUIDQuery
from course_reviews.application.validation import Validator
from course_reviews.domain.course_review import CourseReview
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.domain.value_objects import Comment, CourseRating
from course_reviews.exceptions import (
    ConstraintViolationException,
    RelatedResourceIsNotResolvedException,
    ResourceNotFoundException,
)
from course_reviews.infrastructure.event_bus import EventBus
from course_reviews.infrastructure.repositories import (
    CourseReviewRepository,
    ReviewableCourseRepository,
    ReviewerRepository,
)
from course_reviews.integration_events.events import CourseRatingRecalculatedIntegrationEvent

COURSE_UUID = UUID("123e4567-e89b-12d3-a456-426655440001")


@pytest.fixture
def course_review_repository() -> Mock:
    return create_autospec(CourseReviewRepository, instance=True)


@pytest.fixture
def reviewable_course_repository() -> Mock:
    return create_autospec(ReviewableCourseRepository, instance=True)


@pytest.fixture
def reviewer_repository() -> Mock:
    return create_autospec(ReviewerRepository, instance=True)


# --- ReviewCourseCommandHandler -------------------------------------------------------------------------------------


@pytest.fixture
def recalculated_events() -> tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]]:
    bus = EventBus()
    events: list[CourseRatingRecalculatedIntegrationEvent] = []
    bus.subscribe(CourseRatingRecalculatedIntegrationEvent, events.append)
    return bus, events


def test_review_course_handle_valid_command_review_saved_and_identifier_returned(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    recalculated_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, _ = recalculated_events
    factory = create_autospec(CourseReviewFactory, instance=True)
    review = CourseReview.create(course=1, reviewer=2, rating=4.0, comment="comment")
    factory.create_from.return_value = review
    command = ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0, comment="comment")
    sut = ReviewCourseCommandHandler(course_review_repository, factory, reviewable_course_repository, bus)

    # when
    result = sut.handle(command)

    # then
    assert result == review.to_identifier()
    factory.create_from.assert_called_once_with(command)
    course_review_repository.save.assert_called_once_with(review)


def test_review_course_handle_event_carries_recalculated_average_for_reviews_course(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    recalculated_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = recalculated_events
    factory = create_autospec(CourseReviewFactory, instance=True)
    factory.create_from.return_value = CourseReview.create(course=11, reviewer=2, rating=4.0, comment=None)
    course = ReviewableCourse(COURSE_UUID)
    course.id = 11
    reviewable_course_repository.find_by_id.return_value = course
    course_review_repository.average_rating.return_value = 3.5
    sut = ReviewCourseCommandHandler(course_review_repository, factory, reviewable_course_repository, bus)

    # when
    sut.handle(ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0))

    # then
    reviewable_course_repository.find_by_id.assert_called_once_with(11)
    course_review_repository.average_rating.assert_called_once_with(11)
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=3.5)]


def test_review_course_handle_event_published_after_review_saved(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
) -> None:
    # given
    factory = create_autospec(CourseReviewFactory, instance=True)
    factory.create_from.return_value = CourseReview.create(course=11, reviewer=2, rating=4.0, comment=None)
    course = ReviewableCourse(COURSE_UUID)
    course.id = 11
    reviewable_course_repository.find_by_id.return_value = course
    course_review_repository.average_rating.return_value = 4.0
    bus = create_autospec(EventBus, instance=True)
    parent = Mock()
    parent.attach_mock(course_review_repository.save, "save")
    parent.attach_mock(bus.publish, "publish")
    sut = ReviewCourseCommandHandler(course_review_repository, factory, reviewable_course_repository, bus)

    # when
    sut.handle(ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0))

    # then
    assert [call[0] for call in parent.mock_calls] == ["save", "publish"]


def test_review_course_handle_factory_raises_nothing_saved_and_no_event_published(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    recalculated_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = recalculated_events
    factory = create_autospec(CourseReviewFactory, instance=True)
    factory.create_from.side_effect = RelatedResourceIsNotResolvedException("Course cannot be found")
    sut = ReviewCourseCommandHandler(course_review_repository, factory, reviewable_course_repository, bus)

    # when / then
    with pytest.raises(RelatedResourceIsNotResolvedException):
        sut.handle(ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0))
    course_review_repository.save.assert_not_called()
    assert published == []


def test_review_course_handle_course_projection_missing_review_saved_but_no_event_published(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    recalculated_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = recalculated_events
    factory = create_autospec(CourseReviewFactory, instance=True)
    review = CourseReview.create(course=11, reviewer=2, rating=4.0, comment=None)
    factory.create_from.return_value = review
    reviewable_course_repository.find_by_id.return_value = None
    sut = ReviewCourseCommandHandler(course_review_repository, factory, reviewable_course_repository, bus)

    # when
    result = sut.handle(ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0))

    # then
    assert result == review.uuid
    course_review_repository.save.assert_called_once_with(review)
    reviewable_course_repository.find_by_id.assert_called_once_with(11)
    course_review_repository.average_rating.assert_not_called()
    assert published == []


def test_review_course_handle_save_raises_no_event_published(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    recalculated_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = recalculated_events
    factory = create_autospec(CourseReviewFactory, instance=True)
    factory.create_from.return_value = CourseReview.create(course=11, reviewer=2, rating=4.0, comment=None)
    course_review_repository.save.side_effect = RuntimeError("db down")
    sut = ReviewCourseCommandHandler(course_review_repository, factory, reviewable_course_repository, bus)

    # when / then
    with pytest.raises(RuntimeError, match="db down"):
        sut.handle(ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0))
    reviewable_course_repository.find_by_id.assert_not_called()
    assert published == []


# --- UpdateCourseReviewCommandHandler (edge cases beyond the Java test port) -----------------------------------------


@pytest.fixture
def existing_review(course_review_repository: Mock) -> CourseReview:
    review = CourseReview.create(course=11, reviewer=22, rating=4.0, comment="comment")
    course_review_repository.find_by_uuid.return_value = review
    return review


@pytest.fixture
def update_events() -> tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]]:
    bus = EventBus()
    events: list[CourseRatingRecalculatedIntegrationEvent] = []
    bus.subscribe(CourseRatingRecalculatedIntegrationEvent, events.append)
    return bus, events


def test_update_handle_course_projection_missing_review_saved_but_no_event_published(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    existing_review: CourseReview,
    update_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = update_events
    reviewable_course_repository.find_by_id.return_value = None
    sut = UpdateCourseReviewCommandHandler(Validator(), course_review_repository, reviewable_course_repository, bus)

    # when
    sut.handle(UpdateCourseReviewCommand(uuid=existing_review.uuid, rating=2.0, comment=None))

    # then
    course_review_repository.save.assert_called_once_with(existing_review)
    reviewable_course_repository.find_by_id.assert_called_once_with(11)
    course_review_repository.average_rating.assert_not_called()
    assert published == []


def test_update_handle_event_carries_recalculated_average_for_reviews_course(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    existing_review: CourseReview,
    update_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = update_events
    course = ReviewableCourse(COURSE_UUID)
    course.id = 11
    reviewable_course_repository.find_by_id.return_value = course
    course_review_repository.average_rating.return_value = 2.75
    sut = UpdateCourseReviewCommandHandler(Validator(), course_review_repository, reviewable_course_repository, bus)

    # when
    sut.handle(UpdateCourseReviewCommand(uuid=existing_review.uuid, rating=2.0, comment="edited"))

    # then
    course_review_repository.average_rating.assert_called_once_with(11)
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=2.75)]
    assert existing_review.rating == CourseRating(2.0)
    assert existing_review.comment == Comment("edited")


def test_update_handle_event_published_after_review_saved(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    existing_review: CourseReview,
) -> None:
    # given
    course = ReviewableCourse(COURSE_UUID)
    course.id = 11
    reviewable_course_repository.find_by_id.return_value = course
    course_review_repository.average_rating.return_value = 2.0
    bus = create_autospec(EventBus, instance=True)
    parent = Mock()
    parent.attach_mock(course_review_repository.save, "save")
    parent.attach_mock(bus.publish, "publish")
    sut = UpdateCourseReviewCommandHandler(Validator(), course_review_repository, reviewable_course_repository, bus)

    # when
    sut.handle(UpdateCourseReviewCommand(uuid=existing_review.uuid, rating=2.0))

    # then
    assert [call[0] for call in parent.mock_calls] == ["save", "publish"]


def test_update_handle_unknown_review_and_invalid_rating_not_found_takes_precedence(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
) -> None:
    # given
    course_review_repository.find_by_uuid.return_value = None
    command = UpdateCourseReviewCommand.model_construct(uuid=uuid4(), rating=99, comment=None)
    sut = UpdateCourseReviewCommandHandler(
        Validator(), course_review_repository, reviewable_course_repository, EventBus()
    )

    # when / then
    with pytest.raises(ResourceNotFoundException):
        sut.handle(command)


def test_update_handle_invalid_command_review_left_unmodified(
    course_review_repository: Mock,
    reviewable_course_repository: Mock,
    existing_review: CourseReview,
    update_events: tuple[EventBus, list[CourseRatingRecalculatedIntegrationEvent]],
) -> None:
    # given
    bus, published = update_events
    sut = UpdateCourseReviewCommandHandler(Validator(), course_review_repository, reviewable_course_repository, bus)
    command = UpdateCourseReviewCommand.model_construct(uuid=existing_review.uuid, rating=7, comment="new")

    # when / then
    with pytest.raises(ConstraintViolationException):
        sut.handle(command)
    assert existing_review.rating == CourseRating(4.0)
    assert existing_review.comment == Comment("comment")
    assert published == []


# --- ListCourseReviewsByCourseUUIDQueryHandler ----------------------------------------------------------------------


def test_list_query_handle_delegates_to_repository(course_review_repository: Mock) -> None:
    # given
    dto = CourseReviewDTO(uuid=uuid4(), course=COURSE_UUID, username="username", comment=None, rating=3.0)
    course_review_repository.list_course_reviews.return_value = [dto]
    sut = ListCourseReviewsByCourseUUIDQueryHandler(course_review_repository)

    # when
    result = sut.handle(ListCourseReviewsByCourseUUIDQuery(course_uuid=COURSE_UUID))

    # then
    assert result == [dto]
    course_review_repository.list_course_reviews.assert_called_once_with(COURSE_UUID)


def test_list_query_handle_no_reviews_empty_list(course_review_repository: Mock) -> None:
    course_review_repository.list_course_reviews.return_value = []
    sut = ListCourseReviewsByCourseUUIDQueryHandler(course_review_repository)

    assert sut.handle(ListCourseReviewsByCourseUUIDQuery(course_uuid=uuid4())) == []


# --- Replication handlers -------------------------------------------------------------------------------------------


def test_create_reviewable_course_handle_saves_projection_with_original_id(
    reviewable_course_repository: Mock,
) -> None:
    # given
    reviewable_course_repository.save.side_effect = lambda course: course
    sut = CreateReviewableCourseCommandHandler(reviewable_course_repository)

    # when
    result = sut.handle(COURSE_UUID)

    # then
    assert isinstance(result, ReviewableCourse)
    assert result.original_course_id == COURSE_UUID
    reviewable_course_repository.save.assert_called_once_with(result)


def test_create_reviewer_handle_saves_projection_with_username(reviewer_repository: Mock) -> None:
    # given
    reviewer_repository.save.side_effect = lambda reviewer: reviewer
    sut = CreateReviewerCommandHandler(reviewer_repository)

    # when
    result = sut.handle("username")

    # then
    assert isinstance(result, Reviewer)
    assert result.username == "username"
    reviewer_repository.save.assert_called_once_with(result)


# --- CurrentUserAsReviewer ------------------------------------------------------------------------------------------


def test_current_user_as_reviewer_known_username_reviewer_returned(reviewer_repository: Mock) -> None:
    # given
    reviewer = Reviewer("username")
    reviewer.id = 5
    reviewer_repository.find_by_username.return_value = reviewer
    sut = CurrentUserAsReviewer(reviewer_repository, "username")

    # when
    result = sut.user_as_reviewer()

    # then
    assert result is reviewer
    assert sut.username == "username"
    reviewer_repository.find_by_username.assert_called_once_with("username")


def test_current_user_as_reviewer_unknown_username_related_resource_is_not_resolved_exception(
    reviewer_repository: Mock,
) -> None:
    # given
    reviewer_repository.find_by_username.return_value = None
    sut = CurrentUserAsReviewer(reviewer_repository, "ghost")

    # when / then
    with pytest.raises(RelatedResourceIsNotResolvedException, match="username = ghost"):
        sut.user_as_reviewer()


def test_factory_create_from_unresolved_reviewer_related_resource_is_not_resolved_exception(
    reviewable_course_repository: Mock, reviewer_repository: Mock
) -> None:
    # given
    course = ReviewableCourse(COURSE_UUID)
    course.id = 1
    reviewable_course_repository.find_by_original_course_id.return_value = course
    reviewer_repository.find_by_username.return_value = None
    current_user = CurrentUserAsReviewer(reviewer_repository, "ghost")
    sut = CourseReviewFactory(Validator(), current_user, reviewable_course_repository)

    # when / then
    with pytest.raises(RelatedResourceIsNotResolvedException, match="Reviewer cannot be found"):
        sut.create_from(ReviewCourseCommand(course_id=COURSE_UUID, rating=4.0))


# --- CourseReviewChecker --------------------------------------------------------------------------------------------


@pytest.mark.parametrize("is_reviewer", [True, False])
def test_checker_has_access_delegates_to_repository(course_review_repository: Mock, is_reviewer: bool) -> None:
    # given
    review_uuid = uuid4()
    course_review_repository.is_reviewer.return_value = is_reviewer
    sut = CourseReviewChecker(course_review_repository)

    # when
    result = sut.has_access("username", review_uuid)

    # then
    assert result is is_reviewer
    course_review_repository.is_reviewer.assert_called_once_with(review_uuid, "username")
