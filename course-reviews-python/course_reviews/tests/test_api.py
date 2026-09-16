"""Port of ``web/api/CourseReviewApiTest.java`` using FastAPI's TestClient."""

from __future__ import annotations

from collections.abc import Iterator
from uuid import UUID

import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session, sessionmaker

from course_reviews.application.checker import CourseReviewChecker
from course_reviews.infrastructure.database import get_session
from course_reviews.integration_events.events import CourseRatingRecalculatedIntegrationEvent
from course_reviews.tests.conftest import execute_script
from course_reviews.web.dependencies import event_bus, get_course_review_checker
from course_reviews.web.main import app

COURSE_UUID = UUID("123e4567-e89b-12d3-a456-426655440001")

# Port of web/src/test/resources/insert_data.sql
INSERT_DATA_SQL = """
DELETE FROM course_review;
DELETE FROM reviewable_course;
DELETE FROM reviewer;

INSERT INTO reviewable_course (original_course_id) VALUES ('123e4567e89b12d3a456426655440001');
INSERT INTO reviewer (username) VALUES ('username');
INSERT INTO reviewer (username) VALUES ('another-user');
"""


def student_auth(username: str = "username") -> dict[str, str]:
    """``SignUpHelper.signUpStudent()`` analogue: the simplified auth trusts the bearer token as the username."""
    return {"Authorization": f"Bearer {username}"}


@pytest.fixture
def client(session_factory: sessionmaker[Session]) -> Iterator[TestClient]:
    with session_factory() as session:
        execute_script(session, INSERT_DATA_SQL)

    def override_get_session() -> Iterator[Session]:
        session = session_factory()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    app.dependency_overrides[get_session] = override_get_session
    with TestClient(app) as client:
        yield client
    app.dependency_overrides.clear()


@pytest.fixture
def published() -> Iterator[list[CourseRatingRecalculatedIntegrationEvent]]:
    events: list[CourseRatingRecalculatedIntegrationEvent] = []
    event_bus.subscribe(CourseRatingRecalculatedIntegrationEvent, events.append)
    yield events
    event_bus.unsubscribe(CourseRatingRecalculatedIntegrationEvent, events.append)


def create_review(client: TestClient, body: dict[str, object], username: str = "username") -> UUID:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json=body, headers=student_auth(username))
    assert response.status_code == 201, response.text
    return UUID(response.json()["uuid"])


def test_reviews_valid_request_reviews(client: TestClient) -> None:
    review_uuid = create_review(client, {"rating": 3.2})

    response = client.get(f"/courses/{COURSE_UUID}/reviews", headers=student_auth())

    assert response.status_code == 200
    body = response.json()
    assert body[0]["course"] == str(COURSE_UUID)
    assert body[0]["uuid"] == str(review_uuid)
    assert body[0]["username"] == "username"
    assert body[0]["rating"] == 3.2
    assert body[0]["comment"] is None


def test_review_valid_request_created(client: TestClient) -> None:
    response = client.post(
        f"/courses/{COURSE_UUID}/reviews",
        json={"reviewer": "username", "rating": 3.2},
        headers=student_auth(),
    )

    assert response.status_code == 201
    UUID(response.json()["uuid"])


def test_update_valid_request_no_content(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    review_uuid = create_review(client, {"rating": 3.2})

    response = client.put(
        f"/courses/{COURSE_UUID}/reviews/{review_uuid}",
        json={"comment": "comment2", "rating": 3.5},
        headers=student_auth(),
    )

    assert response.status_code == 204
    assert response.content == b""
    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert listed == [
        {
            "uuid": str(review_uuid),
            "course": str(COURSE_UUID),
            "username": "username",
            "comment": "comment2",
            "rating": 3.5,
        }
    ]
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=3.5)]


def test_review_invalid_rating_bad_request(client: TestClient) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": 6}, headers=student_auth())

    assert response.status_code == 400
    assert response.json()["errors"]


def test_review_comment_too_long_bad_request(client: TestClient) -> None:
    body = {"rating": 3.0, "comment": "x" * 101}
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json=body, headers=student_auth())

    assert response.status_code == 400
    assert response.json()["errors"] == ["comment: String should have at most 100 characters"]


def test_update_comment_too_long_bad_request(client: TestClient) -> None:
    review_uuid = create_review(client, {"rating": 3.2})

    body = {"rating": 3.0, "comment": "x" * 101}
    response = client.put(f"/courses/{COURSE_UUID}/reviews/{review_uuid}", json=body, headers=student_auth())

    assert response.status_code == 400


def test_openapi_documents_400_error_response_and_auth_schemes(client: TestClient) -> None:
    schema = client.get("/openapi.json").json()

    post = schema["paths"]["/courses/{uuid}/reviews"]["post"]
    assert "422" not in post["responses"]
    assert post["responses"]["400"]["content"]["application/json"]["schema"] == {
        "$ref": "#/components/schemas/ErrorResponse"
    }
    assert "HTTPValidationError" not in schema["components"]["schemas"]
    assert set(schema["components"]["securitySchemes"]) == {"BearerUsername", "XUsername"}


def test_review_missing_rating_bad_request(client: TestClient) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"comment": "x"}, headers=student_auth())

    assert response.status_code == 400


def test_review_unknown_course_bad_request(client: TestClient) -> None:
    unknown = UUID("123e4567-e89b-12d3-a456-426655440099")
    response = client.post(f"/courses/{unknown}/reviews", json={"rating": 3}, headers=student_auth())

    assert response.status_code == 400
    assert response.json() == {"errors": [f"Course cannot be found by uuid = {unknown}"]}


def test_review_unauthenticated_unauthorized(client: TestClient) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": 3})

    assert response.status_code == 401
    assert response.json() == {"errors": ["Not authenticated"]}


def test_update_another_reviewer_forbidden(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    review_uuid = create_review(client, {"rating": 3.2, "comment": "original"})

    response = client.put(
        f"/courses/{COURSE_UUID}/reviews/{review_uuid}",
        json={"rating": 1.0, "comment": "hijacked"},
        headers=student_auth("another-user"),
    )

    assert response.status_code == 403
    assert response.json() == {"errors": ["Access Denied"]}
    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert (listed[0]["rating"], listed[0]["comment"]) == (3.2, "original")
    assert published == []


def test_update_unknown_review_forbidden(client: TestClient) -> None:
    unknown = UUID("123e4567-e89b-12d3-a456-426655440099")
    response = client.put(f"/courses/{COURSE_UUID}/reviews/{unknown}", json={"rating": 1.0}, headers=student_auth())

    # The ownership check runs first, exactly as @PreAuthorize does in the Java module.
    assert response.status_code == 403


def test_review_x_username_header_authenticates(client: TestClient) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": 2}, headers={"X-Username": "username"})

    assert response.status_code == 201
    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert [r["username"] for r in listed] == ["username"]


def test_review_unknown_reviewer_bad_request(client: TestClient) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": 3}, headers=student_auth("ghost"))

    assert response.status_code == 400
    assert response.json() == {"errors": ["Reviewer cannot be found by username = ghost"]}
    assert client.get(f"/courses/{COURSE_UUID}/reviews").json() == []


def test_review_non_bearer_authorization_unauthorized(client: TestClient) -> None:
    response = client.post(
        f"/courses/{COURSE_UUID}/reviews", json={"rating": 3}, headers={"Authorization": "Basic dXNlcjpwYXNz"}
    )

    assert response.status_code == 401
    assert response.json() == {"errors": ["Not authenticated"]}


@pytest.mark.parametrize("rating", [0, 5])
def test_review_boundary_rating_created_and_listed(client: TestClient, rating: float) -> None:
    review_uuid = create_review(client, {"rating": rating, "comment": "edge"})

    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert listed == [
        {
            "uuid": str(review_uuid),
            "course": str(COURSE_UUID),
            "username": "username",
            "comment": "edge",
            "rating": rating,
        }
    ]


def test_review_negative_rating_bad_request_with_field_error(client: TestClient) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": -0.5}, headers=student_auth())

    assert response.status_code == 400
    errors = response.json()["errors"]
    assert len(errors) == 1
    assert errors[0].startswith("rating: ")


@pytest.mark.parametrize("rating", ["four", None, [4], {"value": 4}])
def test_review_non_numeric_rating_bad_request_names_field(client: TestClient, rating: object) -> None:
    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": rating}, headers=student_auth())

    assert response.status_code == 400
    errors = response.json()["errors"]
    assert len(errors) == 1
    assert errors[0].startswith("rating: ")
    assert client.get(f"/courses/{COURSE_UUID}/reviews").json() == []


def test_review_malformed_course_uuid_bad_request(client: TestClient) -> None:
    response = client.post("/courses/not-a-uuid/reviews", json={"rating": 3}, headers=student_auth())

    assert response.status_code == 400
    assert response.json()["errors"][0].startswith("path.uuid: ")


def test_reviews_unknown_course_empty_list(client: TestClient) -> None:
    unknown = UUID("123e4567-e89b-12d3-a456-426655440099")

    response = client.get(f"/courses/{unknown}/reviews")

    assert response.status_code == 200
    assert response.json() == []


def test_reviews_multiple_reviews_listed_in_insertion_order(client: TestClient) -> None:
    first = create_review(client, {"rating": 1.0})
    second = create_review(client, {"rating": 5.0, "comment": "second"}, username="another-user")

    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()

    assert [(r["uuid"], r["username"], r["rating"], r["comment"]) for r in listed] == [
        (str(first), "username", 1.0, None),
        (str(second), "another-user", 5.0, "second"),
    ]


def test_update_unauthenticated_unauthorized(client: TestClient) -> None:
    review_uuid = create_review(client, {"rating": 3.2})

    response = client.put(f"/courses/{COURSE_UUID}/reviews/{review_uuid}", json={"rating": 1.0})

    assert response.status_code == 401
    assert client.get(f"/courses/{COURSE_UUID}/reviews").json()[0]["rating"] == 3.2


def test_update_invalid_rating_bad_request_and_review_unchanged(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    review_uuid = create_review(client, {"rating": 3.2, "comment": "original"})

    response = client.put(f"/courses/{COURSE_UUID}/reviews/{review_uuid}", json={"rating": 5.5}, headers=student_auth())

    assert response.status_code == 400
    assert response.json()["errors"]
    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert listed[0]["rating"] == 3.2
    assert listed[0]["comment"] == "original"
    assert published == []


def test_update_missing_rating_bad_request(client: TestClient) -> None:
    review_uuid = create_review(client, {"rating": 3.2})

    response = client.put(
        f"/courses/{COURSE_UUID}/reviews/{review_uuid}", json={"comment": "only"}, headers=student_auth()
    )

    assert response.status_code == 400


def test_update_comment_cleared_when_omitted(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    review_uuid = create_review(client, {"rating": 3.0, "comment": "to be cleared"})

    response = client.put(f"/courses/{COURSE_UUID}/reviews/{review_uuid}", json={"rating": 2.0}, headers=student_auth())

    assert response.status_code == 204
    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert listed[0]["comment"] is None
    assert listed[0]["rating"] == 2.0
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=2.0)]


def test_update_event_rating_is_average_over_all_course_reviews(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    own = create_review(client, {"rating": 1.0})
    create_review(client, {"rating": 5.0}, username="another-user")

    response = client.put(f"/courses/{COURSE_UUID}/reviews/{own}", json={"rating": 3.0}, headers=student_auth())

    assert response.status_code == 204
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=4.0)]


def test_update_x_username_header_authenticates_owner(client: TestClient) -> None:
    review_uuid = create_review(client, {"rating": 3.2})

    response = client.put(
        f"/courses/{COURSE_UUID}/reviews/{review_uuid}", json={"rating": 4.0}, headers={"X-Username": "username"}
    )

    assert response.status_code == 204


class _PermissiveChecker(CourseReviewChecker):
    def __init__(self) -> None:
        pass

    def has_access(self, username: str, review_id: UUID) -> bool:
        return True


def test_update_unknown_review_when_ownership_check_passes_not_found(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    unknown = UUID("123e4567-e89b-12d3-a456-426655440099")
    app.dependency_overrides[get_course_review_checker] = _PermissiveChecker

    response = client.put(f"/courses/{COURSE_UUID}/reviews/{unknown}", json={"rating": 1.0}, headers=student_auth())

    assert response.status_code == 404
    assert response.json() == {"errors": [f"Course Review with uuid: {unknown} not found"]}
    assert published == []


def test_update_course_path_segment_not_validated_against_review(
    client: TestClient, published: list[CourseRatingRecalculatedIntegrationEvent]
) -> None:
    """Like ``CourseReviewController``, the course UUID in the path is not cross-checked with the review."""
    review_uuid = create_review(client, {"rating": 3.2})
    other_course = UUID("123e4567-e89b-12d3-a456-426655440099")

    response = client.put(
        f"/courses/{other_course}/reviews/{review_uuid}", json={"rating": 4.5}, headers=student_auth()
    )

    assert response.status_code == 204
    assert client.get(f"/courses/{COURSE_UUID}/reviews").json()[0]["rating"] == 4.5
    assert published == [CourseRatingRecalculatedIntegrationEvent(course_id=COURSE_UUID, rating=4.5)]


def test_reviews_malformed_course_uuid_bad_request(client: TestClient) -> None:
    response = client.get("/courses/not-a-uuid/reviews")

    assert response.status_code == 400
    assert response.json()["errors"][0].startswith("path.uuid: ")


def test_review_comment_at_column_limit_round_trips(client: TestClient) -> None:
    comment = "x" * 100

    review_uuid = create_review(client, {"rating": 3.0, "comment": comment})

    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert [(r["uuid"], r["comment"], r["rating"]) for r in listed] == [(str(review_uuid), comment, 3.0)]


def test_update_comment_too_long_bad_request_names_field_and_review_unchanged(client: TestClient) -> None:
    review_uuid = create_review(client, {"rating": 3.2, "comment": "before"})

    response = client.put(
        f"/courses/{COURSE_UUID}/reviews/{review_uuid}",
        json={"rating": 1.0, "comment": "x" * 101},
        headers=student_auth(),
    )

    assert response.status_code == 400
    assert response.json() == {"errors": ["comment: String should have at most 100 characters"]}
    listed = client.get(f"/courses/{COURSE_UUID}/reviews").json()
    assert [(r["comment"], r["rating"]) for r in listed] == [("before", 3.2)]


def test_review_non_bearer_authorization_falls_back_to_x_username(client: TestClient) -> None:
    headers = {"Authorization": "Basic dXNlcjpwYXNz", "X-Username": "username"}

    response = client.post(f"/courses/{COURSE_UUID}/reviews", json={"rating": 2}, headers=headers)

    assert response.status_code == 201
    assert [r["username"] for r in client.get(f"/courses/{COURSE_UUID}/reviews").json()] == ["username"]


def test_openapi_every_operation_documents_400_and_drops_fastapi_validation_schemas(client: TestClient) -> None:
    schema = client.get("/openapi.json").json()

    bad_request = {
        "description": "Bad Request",
        "content": {"application/json": {"schema": {"$ref": "#/components/schemas/ErrorResponse"}}},
    }
    operations = [op for path in schema["paths"].values() for op in path.values()]
    assert len(operations) == 3
    for operation in operations:
        assert "422" not in operation["responses"]
        assert operation["responses"]["400"] == bad_request
    assert schema["components"]["schemas"]["ErrorResponse"]["properties"]["errors"]["type"] == "array"
    assert {"HTTPValidationError", "ValidationError"}.isdisjoint(schema["components"]["schemas"])


def test_openapi_security_schemes_are_header_api_keys_required_only_on_write_operations(client: TestClient) -> None:
    schema = client.get("/openapi.json").json()

    assert schema["components"]["securitySchemes"]["BearerUsername"] == {
        "type": "apiKey",
        "in": "header",
        "name": "Authorization",
        "description": "Bearer <username>",
    }
    assert schema["components"]["securitySchemes"]["XUsername"] == {
        "type": "apiKey",
        "in": "header",
        "name": "X-Username",
    }
    either_scheme: list[dict[str, list[str]]] = [{"BearerUsername": []}, {"XUsername": []}]
    assert schema["paths"]["/courses/{uuid}/reviews"]["post"]["security"] == either_scheme
    assert schema["paths"]["/courses/{courseUuid}/reviews/{reviewUuid}"]["put"]["security"] == either_scheme
    assert "security" not in schema["paths"]["/courses/{uuid}/reviews"]["get"]


def test_openapi_schema_is_generated_once_and_cached(client: TestClient) -> None:
    first = app.openapi()

    assert app.openapi() is first
    assert client.get("/openapi.json").json() == first
