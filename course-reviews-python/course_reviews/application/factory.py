"""Course Review factory (``CourseReviewFactory.java``)."""

from __future__ import annotations

from course_reviews.application.commands import ReviewCourseCommand
from course_reviews.application.current_user import CurrentUserAsReviewer
from course_reviews.application.validation import Validator
from course_reviews.domain.course_review import CourseReview
from course_reviews.exceptions import RelatedResourceIsNotResolvedException
from course_reviews.infrastructure.repositories import ReviewableCourseRepository


class CourseReviewFactory:
    def __init__(
        self,
        validator: Validator,
        current_user_as_reviewer: CurrentUserAsReviewer,
        reviewable_course_repository: ReviewableCourseRepository,
    ) -> None:
        self._validator = validator
        self._current_user_as_reviewer = current_user_as_reviewer
        self._reviewable_course_repository = reviewable_course_repository

    def create_from(self, command: ReviewCourseCommand) -> CourseReview:
        """Creates a course review from the command.

        :raises ConstraintViolationException: on validation issues
        :raises RelatedResourceIsNotResolvedException: if the course or reviewer cannot be resolved
        """
        self._validator.validate_or_raise(command)

        course = self._reviewable_course_repository.find_by_original_course_id(command.course_id)
        if course is None:
            raise RelatedResourceIsNotResolvedException(f"Course cannot be found by uuid = {command.course_id}")

        reviewer = self._current_user_as_reviewer.user_as_reviewer()

        return CourseReview.create(
            course=course.local_id, reviewer=reviewer.local_id, rating=command.rating, comment=command.comment
        )
