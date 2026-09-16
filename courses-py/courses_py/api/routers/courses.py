"""``/courses`` router: port of ``CourseController.java`` plus read endpoints backed by the query handlers."""

from __future__ import annotations

from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, Response, status

from courses_py.api.dependencies import (
    CurrentPrincipal,
    course_by_uuid_handler,
    create_course_handler,
    list_course_handler,
    publish_course_handler,
    send_course_to_approve_handler,
)
from courses_py.api.schemas import CreateCourseRequest, CreatedCourseResponse, ErrorResponse
from courses_py.application.course.approve import SendCourseToApproveCommand, SendCourseToApproveCommandHandler
from courses_py.application.course.create import CreateCourseCommand, CreateCourseCommandHandler
from courses_py.application.course.publish import PublishCourseCommand, PublishCourseCommandHandler
from courses_py.application.course.query import (
    CourseByUUIDQuery,
    CourseByUUIDQueryHandler,
    ListCourseQuery,
    ListCourseQueryHandler,
)
from courses_py.application.dtos import CourseDTO, CourseLightDTO
from courses_py.application.exceptions import ResourceNotFoundException

router = APIRouter(prefix="/courses", tags=["Courses"])

_ERROR = {"model": ErrorResponse}


@router.post(
    "",
    status_code=status.HTTP_201_CREATED,
    response_model=CreatedCourseResponse,
    summary="Create course",
    responses={400: _ERROR, 403: _ERROR, 500: _ERROR},
)
def create_course(
    request: CreateCourseRequest,
    handler: Annotated[CreateCourseCommandHandler, Depends(create_course_handler)],
) -> CreatedCourseResponse:
    command = CreateCourseCommand(
        name=request.name, description=request.description, curriculum_items=request.curriculum_items
    )
    return CreatedCourseResponse(uuid=handler.handle(command))


@router.put(
    "/{course_uuid}/publish-status",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Publish Course",
    responses={403: _ERROR, 404: _ERROR, 409: _ERROR, 500: _ERROR},
)
def publish_course(
    course_uuid: UUID,
    handler: Annotated[PublishCourseCommandHandler, Depends(publish_course_handler)],
) -> Response:
    handler.handle(PublishCourseCommand(uuid=course_uuid))
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.put(
    "/{course_uuid}/approval-status",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Send Course to approve",
    responses={403: _ERROR, 404: _ERROR, 409: _ERROR, 500: _ERROR},
)
def send_course_to_approve(
    course_uuid: UUID,
    handler: Annotated[SendCourseToApproveCommandHandler, Depends(send_course_to_approve_handler)],
) -> Response:
    handler.handle(SendCourseToApproveCommand(uuid=course_uuid))
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@router.get(
    "",
    response_model=list[CourseLightDTO],
    response_model_by_alias=True,
    summary="List courses",
    responses={400: _ERROR, 403: _ERROR},
)
def list_courses(
    _principal: CurrentPrincipal, handler: Annotated[ListCourseQueryHandler, Depends(list_course_handler)]
) -> list[CourseLightDTO]:
    return handler.handle(ListCourseQuery())


@router.get(
    "/{course_uuid}",
    response_model=CourseDTO,
    response_model_by_alias=True,
    summary="Get course by uuid",
    responses={400: _ERROR, 403: _ERROR, 404: _ERROR},
)
def course_by_uuid(
    course_uuid: UUID,
    _principal: CurrentPrincipal,
    handler: Annotated[CourseByUUIDQueryHandler, Depends(course_by_uuid_handler)],
) -> CourseDTO:
    course = handler.handle(CourseByUUIDQuery(uuid=course_uuid))
    if course is None:
        raise ResourceNotFoundException(f"Course with uuid: {course_uuid} not found")
    return course
