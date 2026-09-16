"""Command/query handlers (``create/``, ``edit/``, ``query/``, ``course/create/``, ``reviewer/create/``)."""

from __future__ import annotations

from uuid import UUID

from course_reviews.application.commands import ReviewCourseCommand, UpdateCourseReviewCommand
from course_reviews.application.dtos import CourseReviewDTO
from course_reviews.application.factory import CourseReviewFactory
from course_reviews.application.queries import ListCourseReviewsByCourseUUIDQuery
from course_reviews.application.validation import Validator
from course_reviews.domain.reviewable_course import ReviewableCourse
from course_reviews.domain.reviewer import Reviewer
from course_reviews.exceptions import ResourceNotFoundException
from course_reviews.infrastructure.event_bus import EventBus
from course_reviews.infrastructure.repositories import (
    CourseReviewRepository,
    ReviewableCourseRepository,
    ReviewerRepository,
)
from course_reviews.integration_events.events import CourseRatingRecalculatedIntegrationEvent


class ReviewCourseCommandHandler:
    def __init__(self, course_review_repository: CourseReviewRepository, course_review_factory: CourseReviewFactory):
        self._course_review_repository = course_review_repository
        self._course_review_factory = course_review_factory

    def handle(self, command: ReviewCourseCommand) -> UUID:
        course_review = self._course_review_factory.create_from(command)
        self._course_review_repository.save(course_review)
        return course_review.to_identifier()


class UpdateCourseReviewCommandHandler:
    def __init__(
        self,
        validator: Validator,
        course_review_repository: CourseReviewRepository,
        reviewable_course_repository: ReviewableCourseRepository,
        event_bus: EventBus,
    ) -> None:
        self._validator = validator
        self._course_review_repository = course_review_repository
        self._reviewable_course_repository = reviewable_course_repository
        self._event_bus = event_bus

    def handle(self, command: UpdateCourseReviewCommand) -> None:
        """Updates a course review; publishes ``CourseRatingRecalculatedIntegrationEvent`` for its course.

        :raises ResourceNotFoundException: course review not found
        :raises ConstraintViolationException: validation issues
        """
        review = self._course_review_repository.find_by_uuid(command.uuid)
        if review is None:
            raise ResourceNotFoundException(f"Course Review with uuid: {command.uuid} not found")

        self._validator.validate_or_raise(command)

        review.update(command.rating, command.comment)
        self._course_review_repository.save(review)

        course = self._reviewable_course_repository.find_by_id(review.course)
        if course is not None:
            self._event_bus.publish(
                CourseRatingRecalculatedIntegrationEvent(
                    course_id=course.original_course_id,
                    rating=self._course_review_repository.average_rating(review.course),
                )
            )


class ListCourseReviewsByCourseUUIDQueryHandler:
    def __init__(self, repository: CourseReviewRepository) -> None:
        self._repository = repository

    def handle(self, query: ListCourseReviewsByCourseUUIDQuery) -> list[CourseReviewDTO]:
        return self._repository.list_course_reviews(query.course_uuid)


class CreateReviewableCourseCommandHandler:
    """Replicates a course from the ``courses`` context (``course/create/CreateReviewableCourseCommandHandler``)."""

    def __init__(self, reviewable_course_repository: ReviewableCourseRepository) -> None:
        self._reviewable_course_repository = reviewable_course_repository

    def handle(self, course_uuid: UUID) -> ReviewableCourse:
        return self._reviewable_course_repository.save(ReviewableCourse(course_uuid))


class CreateReviewerCommandHandler:
    """Replicates a user from the ``users`` context (``reviewer/create/CreateReviewerCommandHandler.java``)."""

    def __init__(self, reviewer_repository: ReviewerRepository) -> None:
        self._reviewer_repository = reviewer_repository

    def handle(self, username: str) -> Reviewer:
        return self._reviewer_repository.save(Reviewer(username))
