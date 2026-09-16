"""Tests for the exception -> HTTP status/body mapping registered in ``web/main.py::create_app``
(the ``GlobalExceptionHandler`` analogue), exercised through throwaway routes so every mapping is reachable."""

from __future__ import annotations

from collections.abc import Iterator

import pytest
from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient

from course_reviews.exceptions import (
    AccessDeniedException,
    ConstraintViolationException,
    RelatedResourceIsNotResolvedException,
    ResourceNotFoundException,
)
from course_reviews.web.main import create_app


@pytest.fixture
def client() -> Iterator[TestClient]:
    app: FastAPI = create_app()

    @app.get("/raise/not-found")
    def raise_not_found() -> None:
        raise ResourceNotFoundException("Course Review with uuid: x not found")

    @app.get("/raise/constraint-violation")
    def raise_constraint_violation() -> None:
        raise ConstraintViolationException(["rating: too big", "course_id: missing"])

    @app.get("/raise/related-resource")
    def raise_related_resource() -> None:
        raise RelatedResourceIsNotResolvedException("Course cannot be found by uuid = x")

    @app.get("/raise/access-denied")
    def raise_access_denied() -> None:
        raise AccessDeniedException("Access Denied")

    @app.get("/raise/http-exception")
    def raise_http_exception() -> None:
        raise HTTPException(status_code=401, detail="Not authenticated", headers={"WWW-Authenticate": "Bearer"})

    @app.get("/raise/http-exception-non-string-detail")
    def raise_http_exception_non_string_detail() -> None:
        raise HTTPException(status_code=418, detail={"reason": "teapot"})

    with TestClient(app) as client:
        yield client


def test_resource_not_found_exception_404_with_errors_body(client: TestClient) -> None:
    response = client.get("/raise/not-found")

    assert response.status_code == 404
    assert response.json() == {"errors": ["Course Review with uuid: x not found"]}


def test_constraint_violation_exception_400_with_each_violation(client: TestClient) -> None:
    response = client.get("/raise/constraint-violation")

    assert response.status_code == 400
    assert response.json() == {"errors": ["rating: too big", "course_id: missing"]}


def test_related_resource_is_not_resolved_exception_400(client: TestClient) -> None:
    response = client.get("/raise/related-resource")

    assert response.status_code == 400
    assert response.json() == {"errors": ["Course cannot be found by uuid = x"]}


def test_access_denied_exception_403(client: TestClient) -> None:
    response = client.get("/raise/access-denied")

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}


def test_http_exception_keeps_status_headers_and_wraps_detail_in_errors(client: TestClient) -> None:
    response = client.get("/raise/http-exception")

    assert response.status_code == 401
    assert response.headers["WWW-Authenticate"] == "Bearer"
    assert response.json() == {"errors": ["Not authenticated"]}


def test_http_exception_non_string_detail_stringified(client: TestClient) -> None:
    response = client.get("/raise/http-exception-non-string-detail")

    assert response.status_code == 418
    assert response.json() == {"errors": ["{'reason': 'teapot'}"]}


def test_request_validation_error_strips_body_prefix_but_keeps_other_locations(client: TestClient) -> None:
    response = client.post("/courses/not-a-uuid/reviews", json={"rating": "abc"}, headers={"X-Username": "u"})

    assert response.status_code == 400
    errors = response.json()["errors"]
    assert len(errors) == 2
    assert any(e.startswith("path.uuid: ") for e in errors)
    assert any(e.startswith("rating: ") for e in errors)
    assert not any(e.startswith("body") for e in errors)
