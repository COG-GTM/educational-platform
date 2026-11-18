"""Exception -> HTTP mapping, mirroring ``GlobalExceptionHandler`` and ``CourseController.onConflictException``."""

from __future__ import annotations

import logging

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from courses_py.api.schemas import ErrorResponse
from courses_py.application.exceptions import (
    AccessDeniedException,
    RelatedResourceIsNotResolvedException,
    ResourceNotFoundException,
)
from courses_py.domain.exceptions import CourseAlreadyApprovedException, CourseCannotBePublishedException
from courses_py.infrastructure.security.jwt import InvalidJwtTokenException

log = logging.getLogger(__name__)


def _error(status_code: int, *messages: str) -> JSONResponse:
    return JSONResponse(status_code=status_code, content=ErrorResponse(errors=list(messages)).model_dump())


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def on_validation_error(request: Request, e: RequestValidationError) -> JSONResponse:
        return _error(status.HTTP_400_BAD_REQUEST, *(str(err["msg"]) for err in e.errors()))

    @app.exception_handler(RelatedResourceIsNotResolvedException)
    async def on_related_resource_not_resolved(
        request: Request, e: RelatedResourceIsNotResolvedException
    ) -> JSONResponse:
        return _error(status.HTTP_400_BAD_REQUEST, str(e))

    @app.exception_handler(ResourceNotFoundException)
    async def on_resource_not_found(request: Request, e: ResourceNotFoundException) -> JSONResponse:
        return _error(status.HTTP_404_NOT_FOUND, str(e))

    @app.exception_handler(CourseCannotBePublishedException)
    async def on_conflict(request: Request, e: CourseCannotBePublishedException) -> JSONResponse:
        return _error(status.HTTP_409_CONFLICT, str(e))

    @app.exception_handler(CourseAlreadyApprovedException)
    async def on_already_approved(request: Request, e: CourseAlreadyApprovedException) -> JSONResponse:
        return _error(status.HTTP_409_CONFLICT, str(e))

    @app.exception_handler(InvalidJwtTokenException)
    async def on_invalid_jwt(request: Request, e: InvalidJwtTokenException) -> JSONResponse:
        # JwtTokenFilter answers 400 for an expired/invalid token.
        return _error(status.HTTP_400_BAD_REQUEST, str(e))

    @app.exception_handler(AccessDeniedException)
    async def on_access_denied(request: Request, e: AccessDeniedException) -> JSONResponse:
        return _error(status.HTTP_403_FORBIDDEN, str(e))

    @app.exception_handler(Exception)
    async def on_exception(request: Request, e: Exception) -> JSONResponse:
        log.exception("Unhandled error")
        return _error(status.HTTP_500_INTERNAL_SERVER_ERROR, str(e))
