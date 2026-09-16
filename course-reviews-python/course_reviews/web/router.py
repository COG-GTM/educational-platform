"""REST endpoints (``CourseReviewController.java``)."""

from __future__ import annotations

from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Response, status

from course_reviews.application.checker import CourseReviewChecker
from course_reviews.application.commands import ReviewCourseCommand, UpdateCourseReviewCommand
from course_reviews.application.dtos import CourseReviewDTO
from course_reviews.application.handlers import (
    ListCourseReviewsByCourseUUIDQueryHandler,
    ReviewCourseCommandHandler,
    UpdateCourseReviewCommandHandler,
)
from course_reviews.application.queries import ListCourseReviewsByCourseUUIDQuery
from course_reviews.exceptions import AccessDeniedException
from course_reviews.web.dependencies import (
    UsernameDep,
    get_course_review_checker,
    get_list_course_reviews_query_handler,
    get_review_course_command_handler,
    get_update_course_review_command_handler,
)
from course_reviews.web.schemas import CourseReviewCreatedResponse, ReviewCourseRequest, UpdateCourseReviewRequest

router = APIRouter(tags=["Course Reviews"])


@router.post(
    "/courses/{uuid}/reviews",
    status_code=status.HTTP_201_CREATED,
    response_model=CourseReviewCreatedResponse,
)
def review(
    uuid: UUID,
    request: ReviewCourseRequest,
    handler: Annotated[ReviewCourseCommandHandler, Depends(get_review_course_command_handler)],
) -> CourseReviewCreatedResponse:
    command = ReviewCourseCommand(course_id=uuid, rating=request.rating, comment=request.comment)
    return CourseReviewCreatedResponse(uuid=handler.handle(command))


@router.get(
    "/courses/{uuid}/reviews",
    status_code=status.HTTP_200_OK,
    response_model=list[CourseReviewDTO],
)
def reviews(
    uuid: UUID,
    handler: Annotated[ListCourseReviewsByCourseUUIDQueryHandler, Depends(get_list_course_reviews_query_handler)],
) -> list[CourseReviewDTO]:
    return handler.handle(ListCourseReviewsByCourseUUIDQuery(course_uuid=uuid))


@router.put(
    "/courses/{courseUuid}/reviews/{reviewUuid}",
    status_code=status.HTTP_204_NO_CONTENT,
    response_class=Response,
)
def update_review(
    courseUuid: UUID,
    reviewUuid: UUID,
    request: UpdateCourseReviewRequest,
    username: UsernameDep,
    checker: Annotated[CourseReviewChecker, Depends(get_course_review_checker)],
    handler: Annotated[UpdateCourseReviewCommandHandler, Depends(get_update_course_review_command_handler)],
) -> Response:
    # @PreAuthorize("hasRole('STUDENT') and @courseReviewChecker.hasAccess(authentication, #c.uuid)")
    if not checker.has_access(username, reviewUuid):
        raise AccessDeniedException("Access Denied")

    command = UpdateCourseReviewCommand(uuid=reviewUuid, rating=request.rating, comment=request.comment)
    handler.handle(command)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
