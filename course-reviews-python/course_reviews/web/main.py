"""FastAPI application entry point: ``uvicorn course_reviews.web.main:app``."""

from __future__ import annotations

from typing import Any

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.openapi.utils import get_openapi
from fastapi.responses import JSONResponse

from course_reviews.exceptions import (
    AccessDeniedException,
    ConstraintViolationException,
    RelatedResourceIsNotResolvedException,
    ResourceNotFoundException,
)
from course_reviews.infrastructure.orm import start_mappers
from course_reviews.web.router import router
from course_reviews.web.schemas import ErrorResponse


def _error(status_code: int, errors: list[str]) -> JSONResponse:
    return JSONResponse(status_code=status_code, content=ErrorResponse(errors=errors).model_dump())


def _document_400_instead_of_422(app: FastAPI) -> None:
    """Validation failures are returned as 400 ``ErrorResponse`` bodies, not FastAPI's default 422."""

    def custom_openapi() -> dict[str, Any]:
        if app.openapi_schema:
            return app.openapi_schema
        schema = get_openapi(title=app.title, version=app.version, description=app.description, routes=app.routes)
        schema.setdefault("components", {}).setdefault("schemas", {})["ErrorResponse"] = (
            ErrorResponse.model_json_schema()
        )
        bad_request = {
            "description": "Bad Request",
            "content": {"application/json": {"schema": {"$ref": "#/components/schemas/ErrorResponse"}}},
        }
        for path in schema["paths"].values():
            for operation in path.values():
                responses = operation.get("responses", {})
                if responses.pop("422", None) is not None:
                    responses["400"] = bad_request
        schema["components"]["schemas"].pop("HTTPValidationError", None)
        schema["components"]["schemas"].pop("ValidationError", None)
        app.openapi_schema = schema
        return schema

    app.openapi = custom_openapi  # type: ignore[method-assign]


def create_app() -> FastAPI:
    start_mappers()

    app = FastAPI(title="Course Reviews", description="Course Reviews API", version="0.0.1")
    app.include_router(router)
    _document_400_instead_of_422(app)

    # Status mapping mirrors the platform's GlobalExceptionHandler.
    @app.exception_handler(RequestValidationError)
    async def on_request_validation_error(_: Request, exc: RequestValidationError) -> JSONResponse:
        errors = [f"{'.'.join(str(loc) for loc in err['loc'] if loc != 'body')}: {err['msg']}" for err in exc.errors()]
        return _error(status.HTTP_400_BAD_REQUEST, errors)

    @app.exception_handler(ConstraintViolationException)
    async def on_constraint_violation(_: Request, exc: ConstraintViolationException) -> JSONResponse:
        return _error(status.HTTP_400_BAD_REQUEST, exc.violations)

    @app.exception_handler(RelatedResourceIsNotResolvedException)
    async def on_related_resource_not_resolved(_: Request, exc: RelatedResourceIsNotResolvedException) -> JSONResponse:
        return _error(status.HTTP_400_BAD_REQUEST, [str(exc)])

    @app.exception_handler(ResourceNotFoundException)
    async def on_resource_not_found(_: Request, exc: ResourceNotFoundException) -> JSONResponse:
        return _error(status.HTTP_404_NOT_FOUND, [str(exc)])

    @app.exception_handler(AccessDeniedException)
    async def on_access_denied(_: Request, exc: AccessDeniedException) -> JSONResponse:
        return _error(status.HTTP_403_FORBIDDEN, [str(exc)])

    @app.exception_handler(HTTPException)
    async def on_http_exception(_: Request, exc: HTTPException) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content=ErrorResponse(errors=[str(exc.detail)]).model_dump(),
            headers=exc.headers,
        )

    return app


app = create_app()
