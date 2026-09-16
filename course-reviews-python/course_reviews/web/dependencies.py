"""FastAPI dependencies wiring session, repositories, handlers, auth and the event bus.

Authentication is a deliberately thin stand-in for the platform's Spring Security/JWT module: the
current username is taken from ``Authorization: Bearer <username>`` (the token *is* the username, no
signature verification) or, alternatively, from an ``X-Username`` header. Roles (``hasRole('STUDENT')``)
are not enforced. Replace ``get_current_username`` when integrating with the real identity provider.
"""

from __future__ import annotations

from typing import Annotated

from fastapi import Depends, Header, HTTPException, status
from sqlalchemy.orm import Session

from course_reviews.application.checker import CourseReviewChecker
from course_reviews.application.current_user import CurrentUserAsReviewer
from course_reviews.application.factory import CourseReviewFactory
from course_reviews.application.handlers import (
    ListCourseReviewsByCourseUUIDQueryHandler,
    ReviewCourseCommandHandler,
    UpdateCourseReviewCommandHandler,
)
from course_reviews.application.validation import Validator
from course_reviews.infrastructure.database import get_session
from course_reviews.infrastructure.event_bus import EventBus
from course_reviews.infrastructure.repositories import (
    CourseReviewRepository,
    ReviewableCourseRepository,
    ReviewerRepository,
)

event_bus = EventBus()
validator = Validator()


def get_event_bus() -> EventBus:
    return event_bus


def get_validator() -> Validator:
    return validator


def get_current_username(
    authorization: Annotated[str | None, Header()] = None,
    x_username: Annotated[str | None, Header()] = None,
) -> str:
    if authorization:
        scheme, _, token = authorization.partition(" ")
        if scheme.lower() == "bearer" and token.strip():
            return token.strip()
    if x_username:
        return x_username
    raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Not authenticated")


SessionDep = Annotated[Session, Depends(get_session)]
UsernameDep = Annotated[str, Depends(get_current_username)]


def get_course_review_repository(session: SessionDep) -> CourseReviewRepository:
    return CourseReviewRepository(session)


def get_reviewable_course_repository(session: SessionDep) -> ReviewableCourseRepository:
    return ReviewableCourseRepository(session)


def get_reviewer_repository(session: SessionDep) -> ReviewerRepository:
    return ReviewerRepository(session)


CourseReviewRepositoryDep = Annotated[CourseReviewRepository, Depends(get_course_review_repository)]
ReviewableCourseRepositoryDep = Annotated[ReviewableCourseRepository, Depends(get_reviewable_course_repository)]
ReviewerRepositoryDep = Annotated[ReviewerRepository, Depends(get_reviewer_repository)]


def get_current_user_as_reviewer(username: UsernameDep, reviewers: ReviewerRepositoryDep) -> CurrentUserAsReviewer:
    return CurrentUserAsReviewer(reviewers, username)


def get_course_review_factory(
    current_user: Annotated[CurrentUserAsReviewer, Depends(get_current_user_as_reviewer)],
    courses: ReviewableCourseRepositoryDep,
    validator: Annotated[Validator, Depends(get_validator)],
) -> CourseReviewFactory:
    return CourseReviewFactory(validator, current_user, courses)


def get_review_course_command_handler(
    reviews: CourseReviewRepositoryDep,
    factory: Annotated[CourseReviewFactory, Depends(get_course_review_factory)],
) -> ReviewCourseCommandHandler:
    return ReviewCourseCommandHandler(reviews, factory)


def get_update_course_review_command_handler(
    reviews: CourseReviewRepositoryDep,
    courses: ReviewableCourseRepositoryDep,
    validator: Annotated[Validator, Depends(get_validator)],
    bus: Annotated[EventBus, Depends(get_event_bus)],
) -> UpdateCourseReviewCommandHandler:
    return UpdateCourseReviewCommandHandler(validator, reviews, courses, bus)


def get_list_course_reviews_query_handler(
    reviews: CourseReviewRepositoryDep,
) -> ListCourseReviewsByCourseUUIDQueryHandler:
    return ListCourseReviewsByCourseUUIDQueryHandler(reviews)


def get_course_review_checker(reviews: CourseReviewRepositoryDep) -> CourseReviewChecker:
    return CourseReviewChecker(reviews)
