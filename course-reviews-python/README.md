# Course Reviews (Python)

Standalone Python port of the `course-reviews` bounded context (see the Java module in
`../course-reviews/`, which remains the reference implementation). The DDD/CQRS structure and the
REST API contract of the Java module are preserved.

## Layout

```
course_reviews/
  domain/               CourseReview aggregate, ReviewableCourse, Reviewer, value objects (ORM-free)
  application/          commands, queries, DTO, factory, handlers, checker, current user, validation
  web/                  FastAPI app, router, request/response schemas, dependencies (incl. auth)
  integration_events/   CourseRatingRecalculatedIntegrationEvent
  infrastructure/       SQLAlchemy engine/session, imperative mappers, repositories, event bus
  tests/                pytest suite (ports of the Java tests)
alembic/                migrations (Liquibase analogue)
```

## Design decisions

- **FastAPI + Uvicorn** replace Spring MVC. Routes and status codes match `CourseReviewController`:
  - `POST /courses/{uuid}/reviews` -> `201 {"uuid": ...}`
  - `GET  /courses/{uuid}/reviews` -> `200 [CourseReviewDTO, ...]`
  - `PUT  /courses/{courseUuid}/reviews/{reviewUuid}` -> `204`
  - Errors are returned as `{"errors": [...]}` with the same status mapping as the platform's
    `GlobalExceptionHandler` (400 validation / unresolved related resource, 403 access denied, 404 not found).
- **SQLAlchemy 2.0 + Alembic** replace JPA/Hibernate + Liquibase. Domain classes are plain Python and are
  mapped *imperatively* (`infrastructure/orm.py`), so the domain stays free of ORM concerns. `CourseRating`
  and `Comment` are mapped as SQLAlchemy composites onto the `rating`/`comment` columns of `course_review`,
  mirroring the JPA `@Embedded` layout. Integer identity PKs are local; UUIDs are the public identifiers.
- **Pydantic v2** models are used for commands, queries, DTOs and request/response bodies. Rating is
  constrained to `0 <= rating <= 5` (`@PositiveOrZero @Max(5)` analogue). The application layer additionally
  re-validates commands through `Validator` (the `jakarta.validation.Validator` analogue), raising
  `ConstraintViolationException`.
- **In-process synchronous event bus** (`infrastructure/event_bus.py`) replaces
  `ApplicationEventPublisher`. `UpdateCourseReviewCommandHandler` publishes
  `CourseRatingRecalculatedIntegrationEvent(course_id, rating)` after a successful update.
- **Simplified authentication.** The Spring Security/JWT module is *not* reimplemented. The current username
  is taken from `Authorization: Bearer <username>` (the token *is* the username; no signature verification) or
  from an `X-Username` header. Missing credentials -> `401`. Roles are not enforced. Ownership of a review on
  `PUT` is enforced by `CourseReviewChecker.has_access` (the `@PreAuthorize` analogue) -> `403`.
  Swap out `web/dependencies.py::get_current_username` when integrating a real identity provider.

## Install

```bash
cd course-reviews-python
python -m venv .venv && source .venv/bin/activate
pip install -e ".[test]"
```

## Database & migrations

The database URL defaults to `sqlite:///./course_reviews.db` and can be overridden with
`COURSE_REVIEWS_DATABASE_URL` (any SQLAlchemy URL).

```bash
alembic upgrade head
```

## Run the server

```bash
uvicorn course_reviews.web.main:app --reload
```

OpenAPI docs are served at `http://127.0.0.1:8000/docs`. In Swagger UI use the **Authorize** button and enter
either `Bearer <username>` (BearerUsername) or a bare username (XUsername); a value typed into an ordinary
`Authorization` header parameter is dropped by Swagger.

Example (the referenced course must exist in `reviewable_course`, and the user in `reviewer` — these are
replicated from the Course and User contexts in the monolith):

```bash
curl -X POST http://127.0.0.1:8000/courses/123e4567-e89b-12d3-a456-426655440000/reviews \
  -H 'Authorization: Bearer username' -H 'Content-Type: application/json' \
  -d '{"rating": 4.5, "comment": "great"}'
```

## Tests

```bash
pytest
```

Repository and API tests run against an in-memory SQLite database (the H2 analogue), seeded like the Java
`course_review.sql` / `insert_data.sql` fixtures.
